(ns saya.modules.window.view
  (:require
   ["ink" :as k]
   ["react" :as React]
   [applied-science.js-interop :as j]
   [archetype.util :refer [<sub >evt]]
   [saya.cli.text-input :refer [text-input]]
   [saya.modules.buffers.subs :as buffer-subs]
   [saya.modules.ui.cursor :refer [cursor]]
   [saya.modules.ui.placeholders :as placeholders]
   [saya.modules.window.events :as window-events]
   [saya.modules.window.subs :as subs]))

(def system-messages
  {:connecting (fn connecting [url]
                 [:> k/Text {:italic true}
                  "Connecting to " url])
   :disconnected (fn disconnected []
                   [:> k/Text {:italic true}
                    "Disconnected."])})

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
         (if (vector? part)
           (into [(system-messages (first part))] (rest part))
           [:> k/Text part])])]]))

(defn- input-window [connr]
  ; TODO: This ought to be persisted in app-db
  (let [[input set-input!] (React/useState "")]
    [:> k/Box {:align-self :bottom
               :width :100%}
     [:> k/Text "> "]
     [text-input {:value input
                  :on-change set-input!
                  :cursor :pipe
                  :on-submit (fn [v]
                               (set-input! "")
                               (>evt [:connection/send {:connr connr
                                                        :text v}]))}]]))

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
          [:> k/Box {:flex-direction :column
                     :height :100%
                     :width :100%}
           [:> k/Box {:ref ref
                      :flex-direction :column
                      :flex-grow 1
                      :width :100%}
            (for [[i line] lines]
              ^{:key [id i]}
              [buffer-line line {:cursor-col (when (= row i) col)}])]
           (if (<sub [::subs/input-focused? id])
             [input-window (<sub [::buffer-subs/->connr bufnr])]
             [placeholders/line])])))))
