(ns phonsole-server.credentials
  (:require [cheshire.core :refer [parse-string]]
            [jerks-whistling-tunes.utils :refer [decode-base-64]]
            [clojure.java.io :as io]
            [environ.core :refer [env]]))

(defn get-auth0-credentials []
  "loads the auth0 credentials from the auth0-credentials.json file (not in git), or from env vars if the file does not exist"
  (let [file (io/file "auth0-credentials.json")]
    (-> (if (.exists file)
          (-> (slurp file)
              (parse-string true))
          (let [keys [:client-id :client-secret :domain]]
            (zipmap keys (map #(->> %
                                    name
                                    (str "auth0-")
                                    keyword
                                    env)
                              keys))))
        (update :client-secret decode-base-64))
    ))
