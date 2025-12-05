(ns saya.modules.perf.events
  (:require
   [re-frame.core :refer [reg-event-fx trim-v]]
   [saya.modules.echo.core :refer [echo-fx]]))

(reg-event-fx
 ::tti-end
 [trim-v]
 (fn [{:keys [db]} [event]]
   (when-not (get-in db [:tti event :end])
     (let [now (js/performance.now)
           tti (- now
                  (get-in db [:tti event :start]))]
       {:db (assoc-in db [:tti event :end] now)
        :fx [(echo-fx (str "[tti" event "]:") tti "ms")]}))))
