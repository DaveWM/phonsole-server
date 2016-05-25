(ns phonsole-server.middlewares
  (:require [cheshire.core :refer [generate-string parse-string]]
            [phonsole-server.credentials :as credentials]
            [clojure.string :refer [blank?] :as str]
            [jerks-whistling-tunes.core :as jwt]
            [jerks-whistling-tunes.sign :refer [hs256]]))

(defn authenticate [handler]
  "Authenticates using auth token in Authorization header, or query param if header not set. Adds the auth token onto the request as :token"
  (fn [request]
    (let [{:keys [headers query-params]} request
          header (-> (get headers "Authorization")
                     (or "")
                     (str/replace #"^\s*Bearer\s+" ""))
          url-param (get query-params "Authorization")
          token (if (blank? header)
                  url-param
                  header)
          valid? (jwt/valid? token
                             (jwt/signature (hs256 (:client-secret credentials/auth0)))
                             (jwt/aud (:client-id credentials/auth0))
                             (jwt/iss (:domain credentials/auth0))
                             jwt/exp)]
      (if (not valid?)
        (-> {:status 401
             :body (generate-string {:reason "Auth token not found or is invalid"})})
        (handler (assoc request :token token))))))

(defn identify [http-post handler]
  "Identifies the current auth0 user from the auth token, and adds the user info onto the request as :user-info. Requires the authentication middleware"
  (fn [request]
    (let [user-info (-> (http-post (str (:domain credentials/auth0) "tokeninfo")
                                       {:body (generate-string {:id_token (:token request)})
                                        :content-type :json})
                            :body
                            (parse-string true))]
      (handler (assoc request :user-info user-info)))))
