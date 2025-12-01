(ns saya.modules.input.events-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [saya.modules.input.events :refer [add-history-entry]]))

(deftest add-history-entry-test
  (testing "Add to empty state"
    (is (= ["al pastor"]
           (add-history-entry nil "al pastor"))))

  (testing "Deduplicate"
    (is (= ["al pastor"]
           (add-history-entry ["al pastor"] "al pastor")))

    (is (= ["al pastor" "birria"]
           (add-history-entry ["al pastor" "birria"]
                              "al pastor")))

    (is (= ["al pastor" "birria"]
           (add-history-entry ["birria" "al pastor"]
                              "al pastor")))))

