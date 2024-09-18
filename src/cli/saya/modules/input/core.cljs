(ns saya.modules.input.core
  (:require
   [clojure.core.match :refer [match]]
   [re-frame.core :refer [reg-event-fx trim-v]]))

(reg-event-fx
 ::on-key
 [trim-v]
 (fn [{{:keys [mode] :as db} :db} [key]]
   (match [mode key]
     [:normal ":"] {:db (assoc db :mode :command)}

     [:command :escape] {:db (assoc db :mode :normal)}

     :else
     {:fx [[:log ["unhandled: " mode key]]]})))
