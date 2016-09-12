(defproject phonsole-server "0.1.0"
  :description "Server for the phonsole application"
  :url "https://phonsole-server.herokuapp.com/"
  :license {:name "GPL V3"
            :url "https://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha12"]
                 [http-kit "2.1.18"]
                 [com.taoensso/sente "1.11.0-alpha4"]
                 [ring/ring-devel "1.4.0"]
                 [ring/ring-core "1.4.0"]
                 [compojure "1.5.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [clj-http "2.2.0"]
                 [cheshire "5.6.2"]
                 [clj-time "0.12.0"]
                 [jerks-whistling-tunes "0.2.4"]
                 [environ "1.0.3"]]
  :main ^:skip-aot phonsole-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
