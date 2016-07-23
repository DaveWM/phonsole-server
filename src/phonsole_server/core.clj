(ns phonsole-server.core
  (:require [org.httpkit.server :refer [run-server]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.keyword-params]
            [ring.middleware.params]
            [compojure.core :refer [defroutes GET POST]]
            [clojure.string :refer [blank? split starts-with? ends-with?] :as str]
            [clojure.set :refer [difference]]
            [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]
            [cheshire.core :refer [generate-string]]
            [phonsole-server.middlewares :refer [authenticate identify]]
            [phonsole-server.uid-helpers :refer [uid-delimiter viewer-identifier parse-uid-string get-differences]]
            [phonsole-server.credentials :refer [get-auth0-credentials]]
            [clj-http.client :as http]
            [environ.core :refer [env]]))


(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {:user-id-fn (fn [request]
                                                                          (str (get-in request [:user-info :user_id])
                                                                               uid-delimiter
                                                                               (:client-id request)
                                                                               (when (get-in request [:params :is-viewer])
                                                                                 (str uid-delimiter viewer-identifier))))})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(defn get-connection-changed-messages [old-uids new-uids]
  "Returns a list of events to send. Each event is a vector of [uid event]"
  (let [[old-user-infos new-user-infos] (->> [old-uids new-uids]
                                             (map (partial map parse-uid-string))
                                             (into []))
        user-info-map (group-by :user-id new-user-infos)
        to-notify (->> (get-differences old-user-infos new-user-infos)
                       (map :user-id)
                       distinct
                       (mapcat (partial get user-info-map)))
        messages (->> to-notify
                      (map (fn [user-info] [(:uid user-info)
                                            [:clients/connected {:connected-clients (->> (:user-id user-info)
                                                                                         (get user-info-map)
                                                                                         (remove :is-viewer?)
                                                                                         (into []))}]]))
                      )
        ]
    messages))

(add-watch connected-uids :watcher
           (fn [key atom {old-uids :any} {new-uids :any}]
             (doseq [message (get-connection-changed-messages old-uids new-uids)]
               (apply chsk-send! message))
             ))

(go-loop []
  (let [{:keys [id ?data uid] [type data :as event] :event} (<! ch-chsk)
        sending-user (parse-uid-string uid)
        send-to-users (->> (:any @connected-uids)
                           (map parse-uid-string)
                           (filter #(= (:user-id sending-user) (:user-id %))))]
    (println event)
    (when id
      (doseq [{:keys [uid]} send-to-users]
        (chsk-send! uid (if (map? data)
                          [type (assoc data :sender sending-user)]
                          event)))))
  (recur))

(defn app [req]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (generate-string {:test  "Hello World!"
                           :user-id (get-in req [:user-info :user_id])})})

(defroutes routes
  (GET "/" req app)
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req)))

(def port (Integer. (or (env :port) 8080)))
(defonce server (atom nil))


(defn start-server! []
  (println "Started on port" port)
  (let [credentials (get-auth0-credentials)
        bound-identify (partial identify http/post credentials)
        bound-authenticate (partial authenticate credentials)]
    (reset! server (run-server
                    (-> #'routes
                        wrap-reload
                        bound-identify
                        bound-authenticate
                        ring.middleware.keyword-params/wrap-keyword-params
                        ring.middleware.params/wrap-params)
                    {:port port}))))

(defn stop-server! []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn restart-server! []
  (stop-server!)
  (start-server!))


(defn -main []
  (start-server!))
