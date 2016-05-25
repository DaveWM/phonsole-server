(ns phonsole-server.uid-helpers-test
  (:require [clojure.test :refer [deftest is testing]]
            [phonsole-server.uid-helpers :as helpers]))

(deftest parse-uid-string
  (let [{:keys [user-id client-id uid]} (helpers/parse-uid-string "123::abc")]
    (is (= user-id "123"))
    (is (= client-id "abc"))
    (is (= uid "123::abc")))
  (is (true? (:is-viewer? (helpers/parse-uid-string "123::abc::VIEWER"))) "is-viewer? should be true when the string ends with VIEWER")
  (is (false? (:is-viewer? (helpers/parse-uid-string "123::abc::invalidstring"))) "is-viewer? should be false if the string doesn't end with the correct viewer identifier")
  (is (false? (:is-viewer? (helpers/parse-uid-string "123::abc"))) "is-viewer? should be false if the string doesn't have a viewer identifier")
  )

(deftest get-differences
  (testing "should return added item"
    (let [result (helpers/get-differences [] [123])]
      (is (= 1 (count result)))
      (is (= 123 (first result)))))
  (testing "should return deleted item"
    (let [result (helpers/get-differences [123] [])]
      (is (= 1 (count result)))
      (is (= 123 (first result)))))
  (testing "should return an empty list if no differences"
    (is (empty? (helpers/get-differences [123] [123])))))

