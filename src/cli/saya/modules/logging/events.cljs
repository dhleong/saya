(ns saya.modules.logging.events
  (:require
   [re-frame.core :refer [path reg-event-db trim-v]]))

(reg-event-db
 ::log
 [trim-v (path :log)]
 (fn [log [timestamp text]]
   ((fnil conj [])
    log
    {:timestamp timestamp
     :text text})))
