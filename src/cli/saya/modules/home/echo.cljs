(ns saya.modules.home.echo
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.ui.placeholders :as placeholders]))

(defn- echo-line [{:keys [message]}]
  [:> k/Text message])

(defn- blocking-window []
  (let [lines (<sub [:echo-lines])]
    [:> k/Box {:flex-direction :column
               :width :100%}
     (for [{:keys [timestamp message] :as line} lines]
       ^{:key [timestamp message]}
       [echo-line line])
     [:> k/Text "Press ENTER to continue"]]))

(defn echo-window []
  (let [lines (<sub [:echo-lines])]
    (case (count lines)
      0 [placeholders/line]
      1 [echo-line (first lines)]
      [blocking-window])))
