(ns saya.modules.input.window
  (:require
   ["ink" :as k]
   ["react" :as React]
   [archetype.util :refer [>evt]]
   [clojure.core.match :refer [match]]
   [reagent.core :as r]
   [saya.cli.text-input :refer [text-input]]
   [saya.modules.input.core :as input]
   [saya.modules.input.events :as events]))

(defn- on-key [{:keys [bufnr input-ref state-ref
                       on-change on-prepare-buffer on-submit]}
               key]
  (match [@state-ref key]
    [:basic :ctrl/f] (do
                       (when on-prepare-buffer
                         (on-prepare-buffer @input-ref))
                       (>evt [::events/set-cmdline-bufnr {:bufnr bufnr
                                                          :on-submit on-submit}])
                       (reset! state-ref :cmdline))
    [:basic :ctrl/c] (let [input @input-ref]
                       ; NOTE: ctrl-c once to clear, again to exit
                       (on-change "")
                       (when-not (seq input)
                         (>evt [::input/on-key key])))
    [:basic :escape] (>evt [::input/on-key key])
    [:cmdline :escape] (reset! state-ref :basic)
    :else nil))

(defn- basic-input-window [{:keys [before input on-change on-key on-submit]}]
  [:> k/Box
   [:> k/Text before
    [text-input {:value input
                 :on-change on-change
                 :on-key on-key
                 :cursor :pipe
                 :on-submit (fn [v]
                              (on-change v)
                              (on-submit v))}]]])

(defn input-window [{:keys [initial-value on-persist-value on-submit
                            on-prepare-buffer before bufnr]}]
  {:pre [(not (and on-persist-value on-prepare-buffer))]}
  (r/with-let [input-ref (atom initial-value)
               state-ref (r/atom :basic)]
    (let [[input set-input!] (React/useState initial-value)
          on-change (React/useCallback
                     (fn [v]
                       (set-input! v)
                       (reset! input-ref v))
                     #js [])
          on-submit (fn [v]
                      (on-change "")
                      (on-submit v))
          on-key (partial on-key {:bufnr bufnr
                                  :input-ref input-ref
                                  :on-prepare-buffer on-prepare-buffer
                                  :on-change on-change
                                  :on-submit on-submit
                                  :state-ref state-ref})
          params {:before before
                  :input input
                  :on-key on-key
                  :on-change on-change
                  :on-submit on-submit}]
      (React/useEffect
       (fn []
         (fn on-dismount []
           (when on-persist-value
             (let [v @input-ref]
               (when-not (= v initial-value)
                 (on-persist-value v))))))
       #js [])

      (case @state-ref
        :basic
        [basic-input-window params]))))
