(ns saya.modules.layout.events
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.layout.core :as layout]
   [saya.modules.layout.fx :as fx]))

(reg-event-fx
 ::set-current-tab-layout
 [unwrap]
 (fn [{:keys [db]} {:keys [layout script-file state-atom]}]
   (let [layout-id 0 ; TODO: multi-tab support
         old-ref (get-in db [:layouts layout-id :layout/state-atom])
         new-ref? (not (identical? state-atom old-ref))
         db-path [:layouts layout-id]
         db' (update-in db db-path
                        assoc
                        :script-file script-file
                        :id layout-id
                        :component layout
                        :state-atom state-atom)]
     {:db (layout/install db' (get-in db' db-path))
      :fx [(when new-ref?
             [::fx/subscribe-to-layout-atom
              {:layout-id layout-id
               :state-atom state-atom}])
           (when new-ref?
             [::fx/unsubscribe-from-layout-atom state-atom])]})))

(reg-event-fx
 ::set-keyed-buffer-contents
 [unwrap]
 (fn [{:keys [db]} {:keys [key content string]}]
   (when-let [bufnr (get-in db [:layout/keys key :bufnr])]
     (let [lines (cond
                   (some? string)
                   (str/split-lines string)

                   (string? content)
                   (str/split-lines content)

                   (vector? content)
                   content

                   (sequential? content)
                   (vec content))]
       {:dispatch [::buffer-events/set-string-lines
                   {:id bufnr
                    :lines lines}]}))))
