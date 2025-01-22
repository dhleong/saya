(ns saya.modules.window.view
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.buffers.subs :as buffer-subs]
   [saya.modules.ui.cursor :refer [cursor]]
   [saya.modules.window.subs :as subs]))

(defn- buffer-line [line {:keys [cursor-col]}]
  (let [cursor-type (case (<sub [:mode])
                      :insert :pipe
                      :block)]
    [:> k/Text
     (for [[i part] (map-indexed vector line)]
       ^{:key i}
       [:<>
        (when (= cursor-col i)
          [cursor cursor-type])
        [:> k/Text part]])]))

(defn window-view [id]
  (when-let [bufnr (<sub [::subs/buffer-id id])]
    (when-let [lines (<sub [::subs/visible-lines {:bufnr bufnr
                                                  :winnr id}])]
      (let [focused? (<sub [::subs/focused? id])
            {:keys [row col]} (when focused?
                                (<sub [::buffer-subs/buffer-cursor id]))]
        [:> k/Box {:flex-direction :column
                   :height :100%
                   :width :100%}
         (for [[i line] lines]
           ^{:key [id i]}
           [buffer-line line {:cursor-col (when (= row i) col)}])]))))
