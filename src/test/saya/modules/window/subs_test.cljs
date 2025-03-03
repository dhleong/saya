(ns saya.modules.window.subs-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [clojure.string :as str]
   [saya.modules.buffers.line :refer [buffer-line]]
   [saya.modules.window.subs :refer [visible-lines]]))

(deftest visible-lines-test
  (testing "No anchor row"
    (is (= [{:row 1 :col 0 :line ["2"]}
            {:row 2 :col 0 :line ["3"]}]

           (visible-lines
            {:height 2
             :width 8
             :anchor-row nil}
            [(buffer-line "1")
             (buffer-line "2")
             (buffer-line "3")]))))

  (testing "Anchor row"
    (is (= [{:row 0 :col 0 :line ["1"]}
            {:row 1 :col 0 :line ["2"]}]

           (visible-lines
            {:height 2
             :width 8
             :anchor-row 1}
            [(buffer-line "1")
             (buffer-line "2")
             (buffer-line "3")]))))

  (testing "Wrap words"
    (is (= [{:row 0 :col 8 :line (str/split "honor of " "")}
            {:row 0 :col 17 :line (str/split "grayskull" "")}]

           (visible-lines
            {:height 2
             :width 9
             :anchor-row 0}
            [(buffer-line "for the honor of grayskull")]))))

  (testing "Wrap words with anchor offset"
    (is (= [{:row 0 :col 0 :line (str/split "for the " "")}
            {:row 0 :col 8 :line (str/split "honor of " "")}]

           (visible-lines
            {:height 2
             :width 9
             :anchor-offset 1
             :anchor-row 0}
            [(buffer-line "for the honor of grayskull")])))))

