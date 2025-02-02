(ns saya.modules.command.view
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub >evt]]
   [saya.modules.command.events :as events]
   [saya.modules.command.subs :as subs]
   [saya.modules.input.window :refer [input-window]]))

(defn command-line-mode-view []
  [:> k/Box {:flex-direction :row}
   [input-window {:initial-value (<sub [::subs/input-text])
                  :bufnr :cmd
                  :on-prepare-buffer #(>evt [::events/prepare-buffer %])
                  :on-submit #(>evt [:submit-raw-command %])
                  :before ":"}]])
