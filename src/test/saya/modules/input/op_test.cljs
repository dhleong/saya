(ns saya.modules.input.op-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [saya.modules.input.normal :refer [delete-operator]]
   [saya.modules.input.op :as op]
   [saya.modules.input.test-helpers :refer [make-keymap-cofx perform-cofx-key
                                            with-keymap-compare-buffer]]))

(deftest delete-operator-op-test
  (testing "Simple delete integration"
    (is (= {:mode :normal}
           (-> (make-keymap-cofx "for |the honor")
               (perform-cofx-key "d")
               (perform-cofx-key "w")
               :db
               (select-keys [:mode :keymap-buffer])))))

  (testing "Linewise delete last line"
    (with-keymap-compare-buffer (get op/full-line-keymap [:full-line])
      "|For the honor of Grayskull!"
      :empty
      :pending-operator #'delete-operator)))

(deftest inner-word-test
  (testing "Delete inner word"
    (with-keymap-compare-buffer op/inner-word
      "For the h|onor of Grayskull!"
      "For the | of Grayskull!"
      :pending-operator #'delete-operator))

  (testing "Inner-word integration"
    (is (= {:mode :operator-pending
            :keymap-buffer ["i"]}
           (-> (make-keymap-cofx "for |the honor")
               (perform-cofx-key "d")
               (perform-cofx-key "i")
               :db
               (select-keys [:mode :keymap-buffer]))))))

(deftest outer-word-test
  (testing "Delete outer word"
    (with-keymap-compare-buffer op/outer-word
      "For the h|onor of Grayskull!"
      "For the |of Grayskull!"
      :pending-operator #'delete-operator)))
