(ns saya.modules.echo.view
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub >evt]]
   [saya.cli.input :refer [use-keys]]
   [saya.modules.echo.events :as events]
   [saya.modules.input.core :as input]
   [saya.modules.input.modes :as modes]))

(defn- echo-line [{:keys [type message]}]
  [:> k/Text (case type
               :exception {:color :white
                           :background-color :red}
               :error {:color :red}
               :warn {:color :orange}
               {})
   message])

(defn- blocking-window []
  (let [lines (<sub [:echo-lines])]
    [:> k/Box (merge
               {:position :absolute
                :flex-direction :column
                :width :100%
                :left 0
                :right 0
                :bottom 0}
               (case (:type (first lines))
                 :exception {:background-color :red}
                 {:background-color :transparent}))
     (for [{:keys [key] :as line} lines]
       ^{:key key}
       [echo-line line])

     ; TODO: Color scheme?
     [:> k/Box {:background-color :transparent}
      [:> k/Text {:color "blue"}
       "Press ENTER to continue"]]

     (use-keys
      (fn echo-acker [key]
        (>evt [::events/ack-echo])
        (when-not (#{:return} key)
          (>evt [::input/on-key key]))))]))

(defn echo-window []
  (let [lines (<sub [:echo-lines])
        mode (<sub [:mode])]
    (cond
      (empty? lines)
      nil

      (or (> (count lines) 1)
          ; If in a command-like mode, we need to use the blocking window
          ; to see the echo, even if it's a single line---and we definitely
          ; want to see error echoes
          (and (modes/command-like? mode)
               (some #(= :error (:type %))
                     lines)))
      [blocking-window]

      :else
      [echo-line (first lines)])))
