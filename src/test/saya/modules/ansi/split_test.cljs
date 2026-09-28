(ns saya.modules.ansi.split-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [saya.modules.ansi.split :as split]))

(deftest styled-chars->strings-test
  (testing "Handle system messages"
    (is (= ["h" "i"]
           (split/styled-chars->strings
            (split/tokens->styled-chars
             (concat
              (split/->ansi-tokens
               "hi")
              [[:system]])))))))

