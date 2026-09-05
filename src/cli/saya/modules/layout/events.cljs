(ns saya.modules.layout.events
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]))

(reg-event-fx
 ::set-current-tab-layout
 [unwrap]
 (fn [{:keys [db]} {:keys [layout state-atom]}]
   {:db (assoc db
               :layout/component layout
               :layout/state-atom state-atom)}))
