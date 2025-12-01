(ns saya.modules.search.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [saya.modules.search.core :refer [in-string]]))

(deftest search-in-string-test
  (testing "Handle negative case"
    (is (nil?
         (in-string "bacon ipsum al pastor"
                    :newer
                    "birria"))))

  (testing "Simple search"
    (is (= [{:at {:col 0}
             :length 9}]
           (in-string "al pastor"
                      :newer
                      "al pastor")))
    (is (= [{:at {:col 12}
             :length 9}]
           (in-string "bacon ipsum al pastor"
                      :newer
                      "al pastor"))))

  (testing "Ansi search"
    (is (= [{:at {:col 0}
             :length 9}]
           (in-string "\u001b[32mal pastor"
                      :newer
                      "al pastor"))))

  (testing "Mixed-in Ansi search"
    (is (= [{:at {:col 0}
             :length 9}]
           (in-string "\u001b[32mal \u001b[33mpastor"
                      :newer
                      "al pastor")))))

