(ns saya.modules.input.fx
  (:require
   [re-frame.core :refer [reg-fx]]))

(reg-fx
 ::perform-on-submit
 (fn [[on-submit text]]
   (on-submit text)))
