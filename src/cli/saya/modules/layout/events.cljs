(ns saya.modules.layout.events
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.layout.core :as layout]
   [saya.modules.layout.fx :as fx]
   [saya.modules.logging.core :refer [log]]))

(reg-event-fx
 ::set-current-tab-layout
 [unwrap]
 (fn [{:keys [db]} {:keys [layout script-file state-atom]}]
   (let [layout-id 0 ; TODO: 
         old-ref (get-in db [:layouts layout-id :layout/state-atom])
         new-ref? (not (identical? state-atom old-ref))
         db' (update-in db [:layouts layout-id]
                        assoc
                        :script-file script-file
                        :layout/id layout-id
                        :layout/component layout
                        :layout/state-atom state-atom)]
     {:db (layout/install db' {:layout/id layout-id
                               :layout/component layout
                               :layout/state-atom state-atom})
      :fx [(when new-ref?
             [::fx/subscribe-to-layout-atom
              {:layout-id layout-id
               :state-atom state-atom}])
           (when new-ref?
             [::fx/unsubscribe-from-layout-atom state-atom])]})))

(reg-event-fx
 ::set-keyed-buffer-contents
 [unwrap]
 (fn [{:keys [db]} {:keys [key string]}]
   (log key "->" (get-in db [:layout/keys key :bufnr]) "?")
   (when-let [bufnr (get-in db [:layout/keys key :bufnr])]
     (let [lines (str/split-lines string)]
       (log key "->" bufnr (count lines) "lines")
       {:dispatch [::buffer-events/set-string-lines
                   {:id bufnr
                    :lines lines}]}))))
