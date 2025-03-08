(ns saya.modules.input.op-test
  (:require
   [cljs.test :refer-macros [deftest testing]]
   [saya.modules.input.normal :refer [delete-operator]]
   [saya.modules.input.op :as op]
   [saya.modules.input.test-helpers :refer [with-keymap-compare-buffer]]))

(deftest delete-operator-op-test
  (testing "Linewise delete last line"
    (with-keymap-compare-buffer (get op/full-line-keymap [:full-line])
      "|For the honor of Grayskull!"
      :empty
      :pending-operator #'delete-operator)))

