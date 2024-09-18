(ns saya.events
  (:require
   [re-frame.core :refer [path reg-event-db reg-event-fx trim-v]]
   [saya.db :as db]))

(reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db}))

(reg-event-db
 ::set-dimens
 [trim-v (path :dimens)]
 (fn [_ [width height]]
   {:width width :height height}))
