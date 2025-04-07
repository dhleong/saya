(ns saya.modules.input.window
  (:require
   ["ink" :as k]
   ["react" :as React]
   [archetype.util :refer [<sub >evt]]
   [clojure.core.match :refer [match]]
   [reagent.core :as r]
   [saya.cli.text-input :refer [text-input]]
   [saya.modules.completion.events :as completion-events]
   [saya.modules.completion.helpers :refer [refresh-completion]]
   [saya.modules.completion.subs :as completion-subs]
   [saya.modules.input.core :as input]
   [saya.modules.input.events :as events]
   [saya.modules.logging.core :refer [log]]))

(defn- on-key [{:keys [bufnr winnr input-ref
                       on-change on-persist-value
                       on-prepare-buffer on-submit]}
               key]
  (match [key]
    [:ctrl/f] (if (not= :cmdline winnr)
                (do
                  (when on-prepare-buffer
                    (on-prepare-buffer @input-ref))
                  (>evt [::events/set-cmdline-bufnr {:bufnr bufnr
                                                     :on-submit on-submit}]))

                ; TODO: echo; also, need to somehow show this *above*
                ; the cmd line
                (log "Invalid in command-line window; <CR> executes, CTRL-C quits"))
    [:ctrl/c] (let [input @input-ref]
                ; NOTE: ctrl-c once to clear, again to exit
                (on-change "")
                (when-not (seq input)
                  (>evt [::input/on-key key])))

    ; See input.core
    [:escape] (let [to-persist @input-ref]
                (on-persist-value to-persist)
                (>evt [::input/on-key key]))
    :else nil))

(defn input-window [{:keys [initial-value on-persist-value on-submit
                            on-prepare-buffer before bufnr
                            completion]}]
  {:pre [(not (and on-persist-value on-prepare-buffer))]}
  (r/with-let [input-ref (atom (or initial-value ""))]
    (let [[input set-input!] (React/useState @input-ref)
          on-change (React/useCallback
                     (fn [v cursor completion-opts]
                       (when completion
                         (refresh-completion completion bufnr v cursor completion-opts))
                       (set-input! v)
                       (reset! input-ref v))
                     #js [])
          on-submit (fn [v]
                      (on-change "")
                      (on-submit v))

          on-key (partial on-key {:bufnr bufnr
                                  :winnr (<sub [:current-winnr])
                                  :input-ref input-ref
                                  :on-persist-value on-persist-value
                                  :on-prepare-buffer on-prepare-buffer
                                  :on-change on-change
                                  :on-submit on-submit})]
      (React/useEffect
       (fn []
         (>evt [::completion-events/set-bufnr bufnr])

         (fn on-dismount []
           (>evt [::completion-events/unset-bufnr bufnr])
           (when on-persist-value
             (let [v @input-ref]
               (when-not (= v initial-value)
                 (on-persist-value v))))))
       #js [])

      [:> k/Box
       [:> k/Text before
        [text-input {:value input
                     :on-change on-change
                     :on-key on-key
                     :cursor :pipe
                     :completion-word (<sub [::completion-subs/word-to-complete])
                     :completion-candidates (<sub [::completion-subs/candidates])
                     :ghost (<sub [::completion-subs/ghost])
                     :on-submit (fn [v]
                                  (on-change v)
                                  (on-submit v))}]]])))
