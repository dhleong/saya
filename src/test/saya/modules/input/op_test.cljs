(ns saya.modules.input.op-test
  (:require
   [cljs.test :refer-macros [deftest testing is]]
   [saya.modules.input.motions.word :refer [small-word-boundary?]]
   [saya.modules.input.normal :refer [delete-operator]]
   [saya.modules.input.op :as op]
   [saya.modules.input.test-helpers :refer [get-cofx-buffer make-keymap-cofx
                                            perform-cofx-key
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
    (with-keymap-compare-buffer (op/inner-word small-word-boundary?)
      "For the h|onor of Grayskull!"
      "For the | of Grayskull!"
      :pending-operator #'delete-operator))

  (testing "Delete inner word at boundaries"
    (with-keymap-compare-buffer (op/inner-word small-word-boundary?)
      "For the |honor of Grayskull!"
      "For the | of Grayskull!"
      :pending-operator #'delete-operator)

    (with-keymap-compare-buffer (op/inner-word small-word-boundary?)
      "For the hono|r of Grayskull!"
      "For the | of Grayskull!"
      :pending-operator #'delete-operator))

  (testing "Inner-word integration"
    (let [cofx (-> (make-keymap-cofx "For the h|onor of Grayskull!")
                   (perform-cofx-key "d")
                   (perform-cofx-key "i"))]
      (is (= {:mode :operator-pending
              :keymap-buffer ["i"]}
             (select-keys (:db cofx) [:mode :keymap-buffer])))
      (is (= "For the | of Grayskull!"
             (get-cofx-buffer (perform-cofx-key cofx "w")))))))

(deftest outer-word-test
  (testing "Delete outer word, preferring trailing whitespace"
    (with-keymap-compare-buffer (op/outer-word small-word-boundary?)
      "For the h|onor of Grayskull!"
      "For the |of Grayskull!"
      :pending-operator #'delete-operator))

  (testing "Delete outer word, taking leading whitespace if no trailing"
    (with-keymap-compare-buffer (op/outer-word small-word-boundary?)
      "For the honor of |Grayskull"
      "For the honor o|f"
      :pending-operator #'delete-operator))

  (testing "Delete outer word on boundaries"
    (with-keymap-compare-buffer (op/outer-word small-word-boundary?)
      "For the |honor of Grayskull"
      "For the |of Grayskull"
      :pending-operator #'delete-operator)
    (with-keymap-compare-buffer (op/outer-word small-word-boundary?)
      "For the hono|r of Grayskull"
      "For the |of Grayskull"
      :pending-operator #'delete-operator)))
