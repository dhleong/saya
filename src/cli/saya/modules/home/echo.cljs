(ns saya.modules.home.echo
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub >evt]]
   [saya.cli.input :refer [use-keys]]
   [saya.modules.home.events :as events]
   [saya.modules.input.core :as input]
   [saya.modules.ui.placeholders :as placeholders]))

(defn- echo-line [{:keys [message]}]
  [:> k/Text message])

(defn- blocking-window []
  (let [lines (<sub [:echo-lines])]
    [:> k/Box {:position :absolute
               :flex-direction :column
               :width :100%
               :left 0
               :right 0
               :bottom 0}
     (for [{:keys [key] :as line} lines]
       ^{:key key}
       [echo-line line])

     ; TODO: Color scheme?
     [:> k/Text {:color "blue"}
      "Press ENTER to continue"]

     (use-keys
      (fn echo-acker [key]
        (>evt [::events/ack-echo])
        (when-not (#{:return} key)
          (>evt [::input/on-key key]))))]))

(defn echo-window []
  (let [lines (<sub [:echo-lines])]
    (case (count lines)
      0 [placeholders/line]
      1 [echo-line (first lines)]
      [:<>
       [placeholders/line]
       [blocking-window]])))
