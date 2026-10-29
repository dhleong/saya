(ns saya.modules.input.views.keymap
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.input.subs :as input-subs]))

(defn keymap-preview []
  (when-let [buffer (<sub [::input-subs/keymap-buffer-string])]
    [:> k/Box {:width 8}
     [:> k/Text buffer]]))
