(ns saya.modules.command.view
  (:require
   ["ink" :as k]
   [archetype.util :refer [>evt]]
   [saya.modules.input.window :refer [input-window]]))

(defn command-line-mode-view []
  [:> k/Box {:flex-direction :row}
   [input-window {:initial-value ""
                  :on-submit #(>evt [:submit-raw-command %])
                  :before ":"}]])
