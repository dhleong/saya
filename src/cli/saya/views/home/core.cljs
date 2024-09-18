(ns saya.views.home.core
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]))

(defn home-view []
  (let [{:keys [width height]} (<sub [:dimens])]
    [:> k/Box {:flex-direction :row
               :width width
               :height height}
     [:> k/Text "Hi from home"]]))
