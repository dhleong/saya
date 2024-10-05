(ns saya.modules.kodachi.events
  (:require
   [day8.re-frame-10x.inlined-deps.re-frame.v1v3v0.re-frame.core :refer [trim-v]]
   [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
 ::initializing
 (fn [db _]
   (assoc db :kodachi :initializing)))

(reg-event-db
 ::ready
 (fn [db _]
   (assoc db :kodachi :ready)))

(reg-event-db
 ::unavailable
 [trim-v]
 (fn [db [err]]
   (-> db
       (assoc :kodachi :unavailable)
       (assoc :kodachi/err err))))

(reg-event-fx
 ::on-error
 [trim-v]
 (fn [_ [_err]]
    ; TODO: Cleanup any connection state, etc.
    ; TODO: Echo an error message
   {:fx [[:saya.modules.kodachi.fx/init]]}))
