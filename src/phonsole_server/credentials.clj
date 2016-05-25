(ns phonsole-server.credentials
  (:require [cheshire.core :refer [parse-string]]
            [jerks-whistling-tunes.utils :refer [decode-base-64]]))

(def auth0 (-> (slurp "auth0-credentials.json")
                   (parse-string true)
                   (update :client-secret decode-base-64)
                   ))
