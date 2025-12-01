(ns saya.modules.search.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [saya.modules.search.core :refer [in-string]]))

(deftest search-in-string-test
  (testing "Handle negative case, of course"
    (is (nil?
         (in-string "bacon ipsum al pastor"
                    :newer
                    "birria"))))

  (testing "Simple search"
    (is (= [{:at {:col 12}
             :length 9}]
           (in-string "bacon ipsum al pastor"
                      :newer
                      "al pastor")))))

