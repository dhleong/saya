(ns saya.views.home.core
  (:require
   ["ink" :as k]))

(defn home-view []
  [:> k/Box {:flex-direction :row}
   [:> k/Text "Hi from home"]])
