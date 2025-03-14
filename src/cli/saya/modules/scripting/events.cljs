(ns saya.modules.scripting.events
  (:require
   [re-frame.core :refer [reg-event-db unwrap]]))

(reg-event-db
 ::assign-script-to-connection
 [unwrap]
 (fn [db {:keys [connection-id script-file]}]
   (assoc-in db [:connection connection-id :script] script-file)))
