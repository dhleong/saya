(ns saya.modules.home.core
  (:require
   ["ink" :as k]
   [saya.modules.logging.view :refer [logging-view]]))

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
   ; TODO: Eventually, hide this by default
   [logging-view]

   [:> k/Box {:flex-grow 1
              :width :100%}
    [home-content]]

   [:> k/Box {:align-self :bottom
              :width :100%}
    [status-bar]]])
