(ns saya.modules.input.core-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [saya.modules.input.test-helpers :refer [get-cofx-buffer make-keymap-cofx
                                            perform-cofx-key]]))

(deftest delete-operator-test
  (testing "Delete operator lands in :normal mode"
    (is (= {:mode :normal}
           (-> (make-keymap-cofx "for |the honor")
               (perform-cofx-key "d")
               (perform-cofx-key "w")
               :db
               (select-keys [:mode])))))

  (testing "Delete operator is clamped correctly"
    (is (= "for the hon|o"
           (-> (make-keymap-cofx "for the hono|r")
               (perform-cofx-key "d")
               (perform-cofx-key "l")
               (get-cofx-buffer))))))

(deftest change-operator-test
  (testing "Change operator lands in :insert mode"
    (is (= {:mode :insert}
           (-> (make-keymap-cofx "for |the honor")
               (perform-cofx-key "c")
               (perform-cofx-key "w")
               :db
               (select-keys [:mode])))))

  (testing "ciw at the end of the line doesn't clamp the cursor"
    (is (= "for the |"
           (-> (make-keymap-cofx "for the |honor")
               (perform-cofx-key "c")
               (perform-cofx-key "i")
               (perform-cofx-key "w")
               (get-cofx-buffer))))))

