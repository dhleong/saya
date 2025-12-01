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

(defn- safely [f & args]
  (let [f (partial f args)]
    (fn safely-wrapper [& extra]
      (try (apply f extra)
           (catch :default e
             (>evt [:echo :error e]))))))

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
                (on-change "" 0)
                (when-not (seq input)
                  (>evt [::input/on-key key])))

    ; See input.core
    [:escape] (let [to-persist @input-ref]
                (when on-persist-value
                  (on-persist-value to-persist))
                (>evt [::input/on-key key]))
    :else nil))

(defn input-window [{:keys [initial-value initial-cursor
                            on-persist-cursor on-persist-value
                            on-submit
                            on-prepare-buffer before bufnr
                            completion]}]
  (r/with-let [input-ref (atom (or initial-value ""))]
    (let [[input set-input!] (React/useState @input-ref)
          on-change (React/useCallback
                     (fn [v cursor completion-opts change-opts]
                       (when completion
                         (refresh-completion completion bufnr v cursor completion-opts))
                       (set-input! v)
                       (when on-persist-value
                         (on-persist-value v change-opts))
                       (when on-persist-cursor
                         (on-persist-cursor cursor change-opts))
                       (reset! input-ref v))
                     #js [on-persist-value on-persist-cursor])
          on-submit (React/useCallback
                     (fn [v]
                       (on-change "" 0 nil {:for-submit? true})
                       (when bufnr
                         (>evt [::events/add-history {:bufnr bufnr
                                                      :entry v}]))
                       (on-submit v))
                     #js [bufnr on-change on-submit])

          on-key (safely on-key {:bufnr bufnr
                                 :winnr (<sub [:current-winnr])
                                 :input-ref input-ref
                                 :on-persist-value on-persist-value
                                 :on-prepare-buffer on-prepare-buffer
                                 :on-change on-change
                                 :on-submit on-submit})]
      (React/useEffect
       (fn on-mount []
         (>evt [::completion-events/set-bufnr bufnr])

         (fn on-dismount []
           (>evt [::completion-events/unset-bufnr bufnr])))
       #js [bufnr])

      [:> k/Box
       [:> k/Text before
        [text-input {:value input
                     :initial-cursor initial-cursor
                     :on-change on-change
                     :on-key on-key
                     :cursor :pipe
                     :completion-word (<sub [::completion-subs/word-to-complete])
                     :completion-candidates (<sub [::completion-subs/candidates])
                     :ghost (<sub [::completion-subs/ghost])
                     :on-submit on-submit}]]])))
