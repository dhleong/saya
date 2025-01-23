(ns saya.modules.window.view
  (:require
   ["ink" :as k]
   ["react" :as React]
   [applied-science.js-interop :as j]
   [archetype.util :refer [<sub >evt]]
   [saya.modules.buffers.subs :as buffer-subs]
   [saya.modules.ui.cursor :refer [cursor]]
   [saya.modules.window.events :as window-events]
   [saya.modules.window.subs :as subs]))

(defn- buffer-line [line {:keys [cursor-col]}]
  (let [cursor-type (case (<sub [:mode])
                      :insert :pipe
                      :block)]
    [:> k/Box {:min-height 0
               :min-width 0
               :width :100%
               :flex-basis 1}
     [:> k/Text {:wrap :truncate-end}
      (for [[i part] (map-indexed vector line)]
        ^{:key i}
        [:<>
         (when (= cursor-col i)
           [cursor cursor-type])
         [:> k/Text part]])]]))

(defn window-view [id]
  (let [ref (React/useRef)]
    (React/useLayoutEffect
     (fn []
       (when-some [el ref.current]
         (j/let [^:js {:keys [width height]} (k/measureElement el)]
           (>evt [::window-events/on-measured {:id 0
                                               :width width
                                               :height height}])))
       js/undefined))

    (when-let [bufnr (<sub [::subs/buffer-id id])]
      (when-let [lines (<sub [::subs/visible-lines {:bufnr bufnr
                                                    :winnr id}])]
        (let [focused? (<sub [::subs/focused? id])
              {:keys [row col]} (when focused?
                                  (<sub [::buffer-subs/buffer-cursor id]))]
          [:> k/Box {:ref ref
                     :flex-direction :column
                     :height :100%
                     :width :100%}
           (for [[i line] lines]
             ^{:key [id i]}
             [buffer-line line {:cursor-col (when (= row i) col)}])])))))
