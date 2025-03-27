(ns saya.modules.logging.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub ::all-logs :-> :log)
(reg-sub ::window-size :-> :log-window-size)

(reg-sub
 ::recent-logs
 :<- [::all-logs]
 :<- [::window-size]
 :-> (fn [[log window-size]]
       (when window-size
         (distinct (take-last window-size log)))))
