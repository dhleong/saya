(ns saya.modules.window.view
  (:require
   ["ink" :as k]
   ["react" :as React]
   [applied-science.js-interop :as j]
   [archetype.util :refer [<sub >evt]]
   [saya.modules.buffers.subs :as buffer-subs]
   [saya.modules.connection.completion :refer [->ConnectionCompletionSource]]
   [saya.modules.input.window :as input-window]
   [saya.modules.ui.cursor :refer [cursor]]
   [saya.modules.ui.placeholders :as placeholders]
   [saya.modules.window.events :as window-events]
   [saya.modules.window.subs :as subs]))

(def system-messages
  {:connecting (fn connecting [uri]
                 [:> k/Text {:italic true}
                  "Connecting to " uri "..."])

   :connected (fn connected [_uri]
                [:> k/Text {:italic true}
                 "Connected!"])

   :disconnected (fn disconnected [uri]
                   [:> k/Text {:italic true}
                    "Disconnected from " uri "."])

   :local-send (fn local-send [text]
                 ; NOTE: If we combine :reset and :italic then
                 ; we only get :reset. By wrapping like this, we
                 ; get both correctly!
                 [:> k/Text {:color :reset}
                  [:> k/Text {:italic true}
                   text]])})

(defn- input-window [connr]
  [input-window/input-window
   {:bufnr [:conn/input connr]
    :initial-value (<sub [::subs/input-text connr])
    :completion (->ConnectionCompletionSource connr)
    :on-persist-value #(>evt [::window-events/set-input-text {:connr connr
                                                              :text %}])
    :on-submit #(>evt [:connection/send {:connr connr
                                         :text %}])}])

(defn- buffer-line [line {:keys [cursor-col input-connr]}]
  (let [cursor-type (case (<sub [:mode])
                      :insert :pipe
                      :block)
        ; We should at least render a blank column, in case
        ; we have a cursor to render there
        line (or (seq line)
                 [""])]
    [:> k/Box {:min-height 0
               :min-width 0
               :width :100%
               :flex-wrap (when input-connr :wrap)
               :align-items :left}
     [:> k/Box {:height 1
                :flex-shrink 0}
      [:> k/Text {:wrap :truncate-end}
       (for [[i part] (map-indexed vector (or (seq line)
                                              [""]))]
         ^{:key i}
         [:<>
          (when (= cursor-col i)
            [cursor cursor-type])
          (if (vector? part)
            (into [(system-messages (first part))] (rest part))
            [:> k/Text part])])

       ; cursor at eol
       (when (= cursor-col (count line))
         [cursor cursor-type])]]
     (when input-connr
       [input-window input-connr])]))

(defn window-view [id]
  (let [ref (React/useRef)]
    (React/useLayoutEffect
     (fn []
       (when-some [el ref.current]
         (j/let [^:js {:keys [width height]} (k/measureElement el)]
           (>evt [::window-events/on-measured {:id id
                                               :width width
                                               :height height}])))
       js/undefined))

    (when-let [bufnr (<sub [::subs/buffer-id id])]
      (when-let [lines (<sub [::subs/visible-lines {:bufnr bufnr
                                                    :winnr id}])]
        (let [focused? (<sub [::subs/focused? id])
              {:keys [row col]} (when focused?
                                  (<sub [::buffer-subs/buffer-cursor bufnr]))
              input-connr (when (<sub [::subs/input-focused? id])
                            (<sub [::buffer-subs/->connr bufnr]))
              last-row (first (last lines))
              scrolled? (<sub [::subs/scrolled? id])]
          [:> k/Box {:flex-direction :column
                     :height :100%
                     :width :100%}
           [:> k/Box {:ref ref
                      :flex-direction :column
                      :flex-grow 1
                      :width :100%}
            (for [[i line] lines]
              ^{:key [id i]}
              [buffer-line
               line
               {:cursor-col (when (= row i) col)
                :input-connr (when (and (= last-row i)
                                        (not scrolled?))
                               input-connr)}])]
           (when scrolled?
             (if input-connr
               [input-window (<sub [::buffer-subs/->connr bufnr])]
               [placeholders/line]))])))))
