(ns phonsole-server.credentials
  (:require [cheshire.core :refer [parse-string]]
            [jerks-whistling-tunes.utils :refer [decode-base-64]]))

(defn get-auth0-credentials []
  "loads the auth0 credentials from the auth0-credentials.json file (not in git)"
  (-> (slurp "auth0-credentials.json")
      (parse-string true)
      (update :client-secret decode-base-64)
      ))
