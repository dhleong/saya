(ns saya.events
  (:require
   [re-frame.core :refer [reg-event-fx]]
   [saya.db :as db]))

(reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db}))

