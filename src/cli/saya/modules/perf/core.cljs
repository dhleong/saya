(ns saya.modules.perf.core
  (:require
   ["react" :as React]
   [re-frame.core :as rf]
   [saya.modules.perf.events :as events]))

(defn tti-end [event-name]
  (rf/dispatch-sync [::events/tti-end event-name])

  ; Convenient for use in useEffect:
  js/undefined)

(defn use-tti-end-effect [event-name]
  (React/useEffect
   (partial tti-end event-name)
   #js []))
