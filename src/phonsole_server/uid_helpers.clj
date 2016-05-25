(ns phonsole-server.uid-helpers
  (:require [clojure.string :refer [split ends-with?]]
            [clojure.set :refer [difference]]))

(def uid-delimiter "::")

(def viewer-identifier "VIEWER")

(defn parse-uid-string [uid]
  "parses a uid string into a map of user info"
  (let [[user-id client-id viewer :as parts]  (split uid (re-pattern uid-delimiter))]
    (-> (zipmap [:user-id :client-id] parts)
        (assoc :is-viewer? (= viewer viewer-identifier))
        (assoc :uid uid))))

(defn get-differences [old new]
  "Gets the differences between a list of old and new user infos"
  (-> (concat
       (difference (set old) (set new))
       (difference (set new) (set old)))))
