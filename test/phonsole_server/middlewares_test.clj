(ns phonsole-server.middlewares-test
  (:require [phonsole-server.middlewares :as middlewares]
            [phonsole-server.credentials :as credentials]
            [clojure.test :refer :all]
            [jerks-whistling-tunes.core :as jwt]
            [jerks-whistling-tunes.sign :refer [hs256 sign]]
            [clj-time.core :refer [now plus minus days]]
            [clojure.string :refer [includes?]]))


(defn make-token [& [claims]]
  (-> (merge  {:aud  (:client-id credentials/auth0)
               :exp (plus (now) (days 1))
               :iss (:domain credentials/auth0)}
              claims)
      (update :exp #(quot (.getMillis %) 1000))
      (jwt/encode (hs256 (:client-secret credentials/auth0)))))

(deftest authenticate
  (let [process-request (middlewares/authenticate identity)]
    (is (function? process-request) "middleware function should return a function")
    (let [valid-token (make-token)] 
      (testing "should return the request when the auth token is given as a header"
        (let [valid-response (process-request {:body "test" :status 200 :headers {"Authorization" (str "Bearer " valid-token)} :query-params {}})]
          (is (= "test" (:body valid-response)))
          (is (= 200 (:status valid-response)))))
      (testing "should return the request when the auth token is given as a query param"
          (let [valid-response (process-request {:body "test" :status 200 :headers {} :query-params {"Authorization" valid-token}})]
            (is (= "test" (:body valid-response)))
            (is (= 200 (:status valid-response)))))
      (is (= valid-token (:token (process-request {:body "test" :status 200 :headers {"Authorization" valid-token}}))) "it should add a :token key to the request"))
    (testing "an invalid token should return a 401 response"
      (is (= 401 (:status (process-request {:body "test" :status 200 :headers {"Authorization" "invalid-token"}}))))
      (is (= 401 (:status (process-request {:body "test" :status 200 :headers {"Authorization" (make-token {:aud "invalid"})}}))) "invalid aud claim")
      (is (= 401 (:status (process-request {:body "test" :status 200 :headers {"Authorization" (make-token {:exp (minus (now) (days 1))})}}))) "expired token")
      (is (= 401 (:status (process-request {:body "test" :status 200 :headers {"Authorization" (make-token {:iss "not valid"})}})))) "invalid issuer"
      )))

;TODO use a mocking framework or similar for this test
(deftest identify
  (let [process-request (middlewares/identify (fn [url {:keys [body content-type]}] (when (and (includes? body "token")
                                                                                               (= content-type :json))
                                                                                      {:body "{\"a\": 1}"}))
                                              identity)]
    (is (function? process-request))
    (is (= {:a 1} (:user-info (process-request {:token "token"}))))))
