(ns saya.modules.window.events
  (:require
   [re-frame.core :refer [reg-event-db unwrap]]))

(reg-event-db
 ::on-measured
 [unwrap]
 (fn [db {:keys [id width height]}]
   (update-in db [:windows id] merge {:width width
                                      :height height})))
