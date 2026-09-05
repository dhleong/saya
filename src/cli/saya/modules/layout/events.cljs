(ns saya.modules.layout.events
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.layout.fx :as fx]))

(reg-event-fx
 ::set-current-tab-layout
 [unwrap]
 (fn [{:keys [db]} {:keys [layout script-file state-atom]}]
   (let [layout-id 0 ; TODO: 
         old-ref (get-in db [:layouts layout-id :layout/state-atom])
         new-ref? (not (identical? state-atom old-ref))]
     {:db (update-in db [:layouts layout-id]
                     assoc
                     :script-file script-file
                     :layout/component layout
                     :layout/state-atom state-atom)
      :fx [(when new-ref?
             [::fx/subscribe-to-layout-atom
              {:layout-id layout-id
               :state-atom state-atom}])
           (when new-ref?
             [::fx/unsubscribe-from-layout-atom state-atom])]})))
