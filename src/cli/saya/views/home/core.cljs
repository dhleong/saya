(ns saya.views.home.core
  (:require
   ["ink" :as k]))

(defn home-view []
  [:> k/Box {:align-self :center
             :flex-direction :column
             :justify-content :center
             :height :100%}
   [:> k/Text "Hi from home"]])
