(ns phonsole-server.middlewares-test
  (:require [phonsole-server.middlewares :as middlewares]
            [clojure.test :refer :all]
            [jerks-whistling-tunes.core :as jwt]
            [jerks-whistling-tunes.sign :refer [hs256 sign]]
            [clj-time.core :refer [now plus minus days]]
            [clojure.string :refer [includes?]]))

(def credentials {:client-secret "verysecretsecret"
                  :client-id "clientid"
                  :domain "domain"})

(defn make-token [& [claims]]
  (-> (merge  {:aud  (:client-id credentials)
               :exp (plus (now) (days 1))
               :iss (:domain credentials)}
              claims)
      (update :exp #(quot (.getMillis %) 1000))
      (jwt/encode (hs256 (:client-secret credentials)))))

(deftest authenticate
  (let [process-request (middlewares/authenticate credentials identity)]
    (is (function? process-request) "middleware function should return a function")
    (let [valid-token (make-token)] 
      (testing "should return the request when the auth token is given as a header"
        (let [valid-response (process-request {:body "test" :status 200 :headers {"authorization" (str "Bearer " valid-token)} :query-params {}})]
          (is (= "test" (:body valid-response)))
          (is (= 200 (:status valid-response)))))
      (testing "should return the request when the auth token is given as a query param"
          (let [valid-response (process-request {:body "test" :status 200 :headers {} :query-params {"Authorization" valid-token}})]
            (is (= "test" (:body valid-response)))
            (is (= 200 (:status valid-response)))))
      (is (= valid-token (:token (process-request {:body "test" :status 200 :headers {"authorization" valid-token}}))) "it should add a :token key to the request"))
    (testing "an invalid token should return a 401 response"
      (is (= 401 (:status (process-request {:body "test" :status 200 :headers {"authorization" "invalid-token"}}))))
      (is (= 401 (:status (process-request {:body "test" :status 200 :headers {"authorization" (make-token {:aud "invalid"})}}))) "invalid aud claim")
      (is (= 401 (:status (process-request {:body "test" :status 200 :headers {"authorization" (make-token {:exp (minus (now) (days 1))})}}))) "expired token")
      (is (= 401 (:status (process-request {:body "test" :status 200 :headers {"authorization" (make-token {:iss "not valid"})}})))) "invalid issuer"
      )))

;TODO use a mocking framework or similar for this test
(deftest identify
  (let [process-request (middlewares/identify (fn [url {:keys [body content-type]}] (when (and (includes? body "token")
                                                                                               (= content-type :json))
                                                                                      {:body "{\"a\": 1}"}))
                                              credentials
                                              identity)]
    (is (function? process-request))
    (is (= {:a 1} (:user-info (process-request {:token "token"}))))))
