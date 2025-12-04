(ns saya.modules.input.core-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [saya.modules.input.test-helpers :refer [make-keymap-cofx perform-cofx-key]]))

(deftest delete-operator-test
  (testing "Delete operator lands in :normal mode"
    (is (= {:mode :normal}
           (-> (make-keymap-cofx "for |the honor")
               (perform-cofx-key "d")
               (perform-cofx-key "w")
               :db
               (select-keys [:mode]))))))

(deftest change-operator-test
  (testing "Change operator lands in :insert mode"
    (is (= {:mode :insert}
           (-> (make-keymap-cofx "for |the honor")
               (perform-cofx-key "c")
               (perform-cofx-key "w")
               :db
               (select-keys [:mode]))))))

