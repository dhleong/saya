(ns saya.modules.input.core
  (:require
   [re-frame.core :refer [reg-event-fx trim-v]]))

(reg-event-fx
 ::on-key
 [trim-v]
 (fn [_ [key]]
   (case key
     ; HACKS: For now. To be removed once we have a way to exit
     :ctrl/c {:fx [[:exit]]}

     nil)))
