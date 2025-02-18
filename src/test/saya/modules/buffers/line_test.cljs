(ns saya.modules.buffers.line-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [saya.modules.buffers.line :refer [buffer-line wrapped-lines]]))

(deftest buffer-line-test
  (testing "Wrap lines"
    (is (= [{:col 0 :line ["f" "o" "r" " "]}
            {:col 4 :line ["t" "h" "e"]}]
           (wrapped-lines
            (buffer-line "for the")
            4)))))

