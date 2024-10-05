(ns saya.modules.buffers.view
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.buffers.subs :as subs]))

(defn- buffer-line [line]
  [:> k/Text
   (for [[i part] (map-indexed vector line)]
     ^{:key i}
     [:> k/Text part])])

(defn buffer-view [id]
  ; TODO
  (when-let [buffer (<sub [::subs/by-id id])]
    [:> k/Box {:flex-direction :column
               :height :100%
               :width :100%}
     (for [[i line] (map-indexed vector (:lines buffer))]
       ^{:key [id i]}
       [buffer-line line])]))
