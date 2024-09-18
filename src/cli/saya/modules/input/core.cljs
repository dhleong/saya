(ns saya.modules.input.core
  (:require
   [re-frame.core :refer [reg-event-db trim-v]]
   [saya.modules.logging.core :refer [log]]))

(reg-event-db
 ::on-key
 [trim-v]
 (fn [db [key]]
   ; HACKS:
   (log key)))
