(ns saya.modules.ansi.wrap-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [saya.modules.ansi.wrap :refer [wrap-ansi-chars]]
            [saya.modules.ansi.split :as split]))

(defn wrap-ansi [s width]
  (-> s
      (split/->ansi-tokens)
      (split/tokens->chars-with-ansi)
      (wrap-ansi-chars width)))

(deftest wrap-ansi-test
  (testing "Wrap, preserving complex ansi"
    (is (= ["\u001b[38;5;002mFor the "
            "\u001b[38;5;002mhonor of "
            "\u001b[38;5;002mGrayskull!"]
           (wrap-ansi
            "\u001b[38;5;002mFor the honor of Grayskull!"
            10))))

  (testing "Hard wrap if needed"
    (is (= ["\u001b[32mFor the "
            "\u001b[32mhonor of "
            "\u001b[32mGrayskull"
            "\u001b[32m!"]
           (wrap-ansi
            "\u001b[32mFor the honor of Grayskull!"
            9)))))

