(ns saya.modules.window.subs-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [clojure.string :as str]
   [saya.modules.buffers.line :refer [buffer-line]]
   [saya.modules.window.subs :refer [visible-lines]]))

(deftest visible-lines-test
  (testing "No anchor row"
    (is (= [{:row 1 :col 0 :line ["2"] :last-of-row? true}
            {:row 2 :col 0 :line ["3"] :last-of-row? true}]

           (visible-lines
            {:height 2
             :width 8
             :anchor-row nil}
            [(buffer-line "1")
             (buffer-line "2")
             (buffer-line "3")]))))

  (testing "Anchor row"
    (is (= [{:row 0 :col 0 :line ["1"] :last-of-row? true}
            {:row 1 :col 0 :line ["2"] :last-of-row? true}]

           (visible-lines
            {:height 2
             :width 8
             :anchor-row 1}
            [(buffer-line "1")
             (buffer-line "2")
             (buffer-line "3")]))))

  (testing "Wrap words"
    (is (= [{:row 0 :col 8 :line (str/split "honor of " "") :last-of-row? false}
            {:row 0 :col 17 :line (str/split "grayskull" "") :last-of-row? true}]

           (visible-lines
            {:height 2
             :width 9
             :anchor-row 0}
            [(buffer-line "for the honor of grayskull")]))))

  (testing "Wrap words with anchor offset"
    (is (= [{:row 0 :col 0 :line (str/split "for the " "") :last-of-row? false}
            {:row 0 :col 8 :line (str/split "honor of " "") :last-of-row? false}]

           (visible-lines
            {:height 2
             :width 9
             :anchor-offset 1
             :anchor-row 0}
            [(buffer-line "for the honor of grayskull")])))))

