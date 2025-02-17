(ns saya.modules.window.subs-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [saya.modules.buffers.line :refer [buffer-line]]
   [saya.modules.window.subs :refer [visible-lines]]))

(deftest visible-lines-test
  (testing "No anchor row"
    (is (= [{:row 1 :line ["2"]}
            {:row 2 :line ["3"]}]

           (visible-lines
            {:height 2
             :anchor-row nil}
            [(buffer-line "1")
             (buffer-line "2")
             (buffer-line "3")])))))

