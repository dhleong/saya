(ns saya.modules.ansi.wrap-test
  (:require
   ["@alcalzone/ansi-tokenize" :as ansi]
   [cljs.test :refer-macros [deftest is testing]]
   [clojure.string :as str]
   [saya.modules.ansi.split :as split]
   [saya.modules.ansi.wrap :as wrap :refer [wrap-ansi-chars]]))

(defn- simplify-line [s]
  (-> (ansi/tokenize s)
      (ansi/styledCharsFromTokens)
      (ansi/styledCharsToString)
      (wrap/trim-suffix "\u001b[39m")))

(defn wrap-ansi [s width]
  (-> s
      (split/->ansi-tokens)
      (split/tokens->chars-with-ansi)
      (wrap-ansi-chars width)
      (->> (map str/join)
           (map simplify-line))))

(deftest word-lengths-test
  (testing "Count words"
    (is (= [3 3 5 2 10]
           (#'wrap/->word-lengths
            (-> "\u001b[38;5;002mFor the honor of Grayskull!"
                (split/->ansi-tokens)
                (split/tokens->chars-with-ansi)))))))

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

