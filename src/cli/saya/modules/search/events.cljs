(ns saya.modules.search.events
  (:require
   [re-frame.core :refer [reg-event-db trim-v]]))

(reg-event-db
 ::submit
 [trim-v]
 (fn [db [_query]]
   ; TODO: Move the cursor
   (-> db
       (assoc :mode :normal)
       (update :buffers dissoc :search))))
