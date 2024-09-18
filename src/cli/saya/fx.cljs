(ns saya.fx
  (:require
   [re-frame.core :refer [reg-fx]]
   [saya.modules.logging.core :refer [log]]))

(reg-fx
 :quit
 (fn [_]
   (js/process.emit "exit")))

(reg-fx
 :log
 (fn [parts]
   (if (coll? vals)
     (apply log parts)
     (log parts))))
