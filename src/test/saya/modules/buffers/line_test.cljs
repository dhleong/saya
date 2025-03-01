(ns saya.modules.buffers.line-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [saya.modules.buffers.line :refer [ansi-continuation buffer-line
                                      wrapped-lines]]))

(deftest wrapped-lines-test
  (testing "Wrap lines"
    (is (= [{:col 0 :line ["f" "o" "r" " "]}
            {:col 4 :line ["t" "h" "e"]}]
           (wrapped-lines
            (buffer-line "for the")
            4))))

  (testing "Preserve system messages"
    (is (= [{:col 0 :line [[:local-send "honor"]]}]
           (wrapped-lines
            (buffer-line {:system [:local-send "honor"]})
            4)))))

(deftest ansi-continuation-test
  (testing "Capture final ansi"
    (is (empty?
         (ansi-continuation
          (buffer-line "for the"))))
    (is (= "\u001b[32m\u001b[42m"
           (ansi-continuation
            (buffer-line "\u001B[32mfor \u001B[42mthe"))))))

