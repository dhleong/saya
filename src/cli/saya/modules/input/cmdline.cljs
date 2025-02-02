(ns saya.modules.input.cmdline
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.window.view :refer [window-view]]))

(defn cmdline-window []
  (when (= :cmdline (:id (<sub [:current-window])))
    [:> k/Box {:height 5
               :flex-direction :column
               :width :100%}
     [window-view :cmdline]]))
