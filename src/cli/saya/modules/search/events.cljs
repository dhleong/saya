(ns saya.modules.search.events
  (:require
   [re-frame.core :refer [reg-event-db trim-v]]
   [saya.modules.buffers.line :refer [buffer-line]]))

(reg-event-db
 ::prepare-buffer
 [trim-v]
 (fn [db [pending-input-line]]
   (assoc-in
    db
    [:buffers :search]
    {:lines (-> (get-in db [:histories :search])
                (->> (map buffer-line))
                (reverse)
                (vec)
                (conj (buffer-line pending-input-line)))})))

(reg-event-db
 ::submit
 [trim-v]
 (fn [db [_query]]
   ; TODO: Move the cursor
   (-> db
       (assoc :mode :normal)
       (update :buffers dissoc :search))))
