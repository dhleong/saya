(ns saya.modules.search.events-test
  (:require
   [archetype.util :refer [>evt]]
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rft]
   [re-frame.db :as rfdb]
   [saya.modules.search.events :as events]))

(deftest cancel-search-test
  (testing "Reset window scroll on search cancel"
    (rft/run-test-sync
     (reset! rfdb/app-db {:search {:original-window {0 {:anchor-offset 0}}}
                          :windows {0 {:anchor-offset 0
                                       :anchor-row 42}}
                          :current-winnr 0})
     (>evt [::events/cancel])
     (is (= {:anchor-offset 0}
            (get-in @rfdb/app-db [:windows 0]))))))

