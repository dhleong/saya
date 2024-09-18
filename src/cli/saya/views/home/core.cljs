(ns saya.views.home.core
  (:require
   ["ink" :as k]))

(defn- home-content []
  [:> k/Box {:flex-direction :column
             :height :100%
             :width :100%
             :justify-content :center
             :align-items :center}
   [:> k/Text "Hi from home"]])

(defn- status-bar []
  [:> k/Text "Status"])

(defn home-view []
  [:> k/Box {:flex-direction :column
             :height :100%
             :width :100%}
   [:> k/Box {:flex-grow 1
              :width :100%}
    [home-content]]

   [:> k/Box {:align-self :bottom
              :width :100%}
    [status-bar]]])
