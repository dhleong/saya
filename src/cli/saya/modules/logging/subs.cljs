(ns saya.modules.logging.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub ::all-logs :-> :log)

(reg-sub
 ::recent-logs
 :<- [::all-logs]
 :-> (fn [log]
       (distinct (take-last 2 log))))
