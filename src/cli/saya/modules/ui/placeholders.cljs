(ns saya.modules.ui.placeholders
  (:require
   ["ink" :as k]))

(defn line []
  [:> k/Box {:min-height 1}
   [:> k/Text "\u001B"]])
