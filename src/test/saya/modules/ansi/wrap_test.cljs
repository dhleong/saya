(ns saya.modules.ansi.wrap-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [saya.modules.ansi.wrap :refer [wrap-ansi]]))

(deftest wrap-ansi-test
  (testing "Wrap, preserving complex ansi"
    (is (= ["\u001b[38;5;002mFor the \u001b[0m"
            "\u001b[38;5;002mhonor of \u001b[0m"
            "\u001b[38;5;002mGrayskull!\u001b[0m"]
           (wrap-ansi
            "\u001b[38;5;002mFor the honor of Grayskull!"
            10))))

  (testing "Hard wrap if needed"
    (is (= ["\u001b[32mFor the \u001b[0m"
            "\u001b[32mhonor of \u001b[0m"
            "\u001b[32mGrayskull\u001b[0m"
            "\u001b[32m!\u001b[0m"]
           (wrap-ansi
            "\u001b[32mFor the honor of Grayskull!"
            9)))))

