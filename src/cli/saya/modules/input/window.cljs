(ns saya.modules.input.window
  (:require
   ["ink" :as k]
   ["react" :as React]
   [archetype.util :refer [>evt]]
   [clojure.core.match :refer [match]]
   [reagent.core :as r]
   [saya.cli.input :refer [use-keys]]
   [saya.cli.text-input :refer [text-input]]
   [saya.modules.input.cmdline :refer [>cmdline-window]]
   [saya.modules.input.core :as input]))

(defn- on-key [{:keys [input-ref state-ref on-change]} key]
  (match [@state-ref key]
    [:basic :ctrl/f] (reset! state-ref :cmdline)
    [:basic :ctrl/c] (let [input @input-ref]
                       ; NOTE: ctrl-c once to clear, again to exit
                       (on-change "")
                       (when-not (seq input)
                         (>evt [::input/on-key key])))
    [:basic :escape] (>evt [::input/on-key key])
    [:cmdline :escape] (reset! state-ref :basic)
    :else nil))

(defn- basic-input-window [{:keys [before input on-change on-submit]}]
  [:> k/Box
   [:> k/Text before
    [text-input {:value input
                 :on-change on-change
                 :cursor :pipe
                 :on-submit (fn [v]
                              (on-change v)
                              (on-submit v))}]]])

(defn- cmdline-input-window [{:keys [before input on-change on-submit]}]
  (let [before [:> k/Text {:dim-color true} before]]
    [:> k/Box {:flex-direction :column-reverse
               :height 5
               :overflow-y :hidden
               :width :100%}
     [:> k/Box {:min-height 1
                :flex-shrink 0
                :width :100%}
      [:> k/Text
       before
       [:> k/Text input]]]

     (for [i (range 0 4)]
       ^{:key i}
       [:> k/Box {:min-height 1
                  :flex-shrink 0
                  :width :100%}
        [:> k/Text
         before

         ; TODO: Render history, if available
         [:> k/Text {:dim-color true
                     :color :blue}
          "~"]]])]))

(defn input-window [{:keys [initial-value on-persist-value
                            on-submit
                            before]}]
  (r/with-let [input-ref (atom initial-value)
               state-ref (r/atom :basic)]
    (let [[input set-input!] (React/useState initial-value)
          on-change (React/useCallback
                     (fn [v]
                       (set-input! v)
                       (reset! input-ref v))
                     #js [])
          params {:before before
                  :input input
                  :on-change on-change
                  :on-submit (fn [v]
                               (on-change "")
                               (on-submit v))}]
      (React/useEffect
       (fn []
         (fn on-dismount []
           (when on-persist-value
             (let [v @input-ref]
               (when-not (= v initial-value)
                 (on-persist-value v))))))
       #js [])

      (use-keys (partial on-key {:input-ref input-ref
                                 :on-change on-change
                                 :state-ref state-ref}))

      (case @state-ref
        :cmdline
        [>cmdline-window
         [cmdline-input-window params]]

        :basic
        [basic-input-window params]))))
