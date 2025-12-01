(ns saya.modules.input.cmdline
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.window.view :refer [window-view]]))

(defn cmdline-window []
  (when (= :cmdline (:id (<sub [:current-window])))
    [:> k/Box {:height 5
               :flex-direction :column
               :border-style :round
               :border-top true
               :border-top-dim-color true
               :border-left false
               :border-right false
               :border-bottom false
               :width :100%}
     [window-view :cmdline]]))
