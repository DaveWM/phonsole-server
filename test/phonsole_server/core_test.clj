(ns phonsole-server.core-test
  (:require [clojure.test :refer :all]
            [phonsole-server.core :refer :all]
            [clojure.set :refer [intersection]]))


(deftest test-connection-changed-messages
  (testing "it should return a message for a newly connected user"
    (let [messages (get-connection-changed-messages [] ["abc::123"])
          [uid [event {user-infos :connected-clients} :as message]] (first messages)]
      (is (= 1 (count messages)))
      (is (= uid "abc::123"))
      (is (= event :clients/connected))
      (is (= 1 (count user-infos)))
      (is (= "abc::123" (get-in user-infos ["123" :uid])))
      ))
  (testing "it should return a message for a newly disconnected user"
    (let [messages (get-connection-changed-messages ["abc::123" "abc::345"] ["abc::123"])
          [uid [event {user-infos :connected-clients} :as message]] (first messages)]
      (is (= 1 (count messages)))
      (is (= uid "abc::123"))
      (is (= event :clients/connected))
      (is (= 1 (count user-infos)))
      (is (= "abc::123" (get-in user-infos ["123" :uid])))
      ))
  (testing "it should not send any messages if no connected clients are affected"
    (let [messages (get-connection-changed-messages ["abc::123" "def::345"] ["def::345"])]
      (is (empty? messages))
      )
    )
  (testing "existing users should get a message if a new client connects"
    (let [messages (get-connection-changed-messages ["abc::123"] ["abc::123" "abc::456"])
          [uid [event {user-infos :connected-clients} :as message]] (first (filter (fn [[uid]] (= uid "abc::123")) messages))]
      (is (= 2 (count messages)))
      (is (some? uid))
      (is (= 2 (count user-infos)))
      (is (= 2 (->> (vals user-infos)
                    (map :uid)
                    set
                    (intersection #{"abc::123" "abc::456"})
                    count)))
      )))
