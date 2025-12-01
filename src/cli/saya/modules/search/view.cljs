(ns saya.modules.search.view
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub >evt]]
   [saya.modules.input.window :refer [input-window]]
   [saya.modules.search.events :as events]
   [saya.modules.search.subs :as subs]))

(defn search-mode-view []
  [:> k/Box {:flex-direction :row}
   ; NOTE: We want to pull the :cmd buffer state for the initial value in case
   ; we're returning from cmdline window via ctrl/c, BUT if we're *entering*
   ; command mode *from* cmdline, we need a blank buffer
   [input-window {:initial-value (when-not (= :search (<sub [:current-bufnr]))
                                   ; TODO: 
                                   (<sub [::subs/input-text]))
                  :bufnr :search
                  ; :completion (->CommandCompletionSource)
                  :on-prepare-buffer #(>evt [::events/prepare-buffer %])
                  ; :on-submit #(>evt [:submit-raw-command %])
                  :on-submit #(>evt [::events/submit %])
                  :before "/"}]])
