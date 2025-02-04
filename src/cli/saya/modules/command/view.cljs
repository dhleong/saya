(ns saya.modules.command.view
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub >evt]]
   [saya.modules.command.completion :refer [->CommandCompletionSource]]
   [saya.modules.command.events :as events]
   [saya.modules.command.subs :as subs]
   [saya.modules.input.window :refer [input-window]]))

(defn command-line-mode-view []
  [:> k/Box {:flex-direction :row}
   ; NOTE: We want to pull the :cmd buffer state for the initial value in case
   ; we're returning from cmdline window via ctrl/c, BUT if we're *entering*
   ; command mode *from* cmdline, we need a blank buffer
   [input-window {:initial-value (when-not (= :cmd (<sub [:current-bufnr]))
                                   (<sub [::subs/input-text]))
                  :bufnr :cmd
                  :completion (->CommandCompletionSource)
                  :on-prepare-buffer #(>evt [::events/prepare-buffer %])
                  :on-submit #(>evt [:submit-raw-command %])
                  :before ":"}]])
