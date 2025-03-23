(ns saya.modules.home.events
  (:require
   [re-frame.core :refer [inject-cofx reg-event-fx]]))

(reg-event-fx
 ::ack-echo
 [(inject-cofx :now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (dissoc :echo-ack-pending-since)
            (assoc :echo-cleared-at now))}))
