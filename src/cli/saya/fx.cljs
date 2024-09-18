(ns saya.fx
  (:require
   [re-frame.core :refer [reg-fx]]
   [saya.modules.logging.core :refer [log]]))

(reg-fx
 :exit
 (fn [_]
   (js/process.emit "exit")))

(reg-fx
 :log
 (fn [& parts]
   (apply log parts)))
