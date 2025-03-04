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

  (testing "Preserve empty lines"
    (is (= [{:col 0 :line []}]
           (wrapped-lines
            (buffer-line)
            4))))

  (testing "Preserve system messages"
    (is (= [{:col 0 :line [[:local-send "honor"]]}]
           (wrapped-lines
            (buffer-line {:system [:local-send "honor"]})
            4)))

    (is (= [{:col 0 :line ["f" "o" "r" [:local-send "honor"]]}]
           (-> (buffer-line "for")
               (conj {:system [:local-send "honor"]})
               (wrapped-lines 20)))))

  (testing "Preserve ansi on split lines"
    (is (= ["\u001b[32mf"
            "\u001b[32mt"]
           (->> (wrapped-lines
                 (buffer-line "\u001b[32mfor the")
                 4)
                (map (comp first :line)))))

    (is (= ["\u001b[38;5;002mf"
            "\u001b[38;5;002mt"]
           (->> (wrapped-lines
                 (buffer-line "\u001b[38;5;002mfor the")
                 4)
                (map (comp first :line))))))

  (testing "Don't create keys due to long splits"
    (is (= [{:col 0 :line ["-" "-" "-" "-"]}]
           (->> (wrapped-lines
                 (buffer-line "----")
                 4))))))

(deftest ansi-continuation-test
  (testing "Capture final ansi"
    (is (empty?
         (ansi-continuation
          (buffer-line "for the"))))
    (is (= "\u001b[32m\u001b[42m"
           (ansi-continuation
            (buffer-line "\u001B[32mfor \u001B[42mthe")))))

  (testing "Handle trailing ansi"
    ; If ansi state gets "reset" or otherwise changed the end of
    ; the line, we should handle that correctly
    (is (= "[38;5;007m"
           (ansi-continuation
            (buffer-line
             "[38;5;006mOpen and close[38;5;007m"))))
    (is (empty?
         (ansi-continuation
          (buffer-line
           "[38;5;006mOpen and close[0m"))))))

