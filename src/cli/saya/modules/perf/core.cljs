(ns saya.modules.perf.core
  (:require
   ["react" :as React]
   [re-frame.core :as rf]
   [saya.modules.perf.events :as events]
   [taoensso.tufte :as tufte]))

(defn tti-end [event-name]
  (rf/dispatch-sync [::events/tti-end event-name])

  ; Convenient for use in useEffect:
  js/undefined)

(defn use-tti-end-effect [event-name]
  (React/useEffect
   (partial tti-end event-name)
   #js []))

(defonce ^:private stats-accumulator (tufte/stats-accumulator))

(defn init! []
  (when (seq js/process.env.PROFILE)
    (tufte/add-handler!
     :main-handler
     (tufte/handler:accumulating stats-accumulator))
    (tufte/set-ns-filter! js/process.env.PROFILE)))

(comment
  (println
   (tufte/format-grouped-pstats (stats-accumulator))))
