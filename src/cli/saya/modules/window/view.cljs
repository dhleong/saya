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
    :on-submit (fn [text]
                 ; NOTE: Ensure input is cleared; on-persist-value *may not*
                 ; be called from the cmdline window. This is kinda hacks,
                 ; but fixing properly in input-window feels... annoying
                 (>evt [::window-events/set-input-text {:connr connr
                                                        :text ""}])
                 (>evt [:connection/send {:connr connr
                                          :text text}]))}])

(defn- buffer-line [{:keys [cursor-col input-line? suffix-text]
                     {:keys [line col]} :line}
                    & children]
  (let [cursor-type (case (<sub [:mode])
                      :insert :pipe
                      :operator-pending :underscore
                      :block)
        ; We should at least render a blank column, in case
        ; we have a cursor to render there
        line (or (seq line)
                 [""])]
    (into
     [:> k/Box {:min-height 0
                :width :100%
                :flex-wrap (when input-line? :wrap)
                :align-items :left}
      [:> k/Box {:height 1
                 :flex-shrink 0
                 :flex-direction :column
                 ; :width (cond
                 ;          (and cursor-col input-line?) 48
                 ;          (not input-line?) :100%)
                 :width (when-not input-line?
                          :100%)}
       [:> k/Text {:wrap :truncate-end}
        (for [[i part] (map-indexed vector line)]
          ^{:key i}
          [:<>
           (when (= cursor-col (+ col i))
             [cursor cursor-type])
           (if (vector? part)
             (into [(system-messages (first part))] (rest part))
             [:> k/Text part])])

         ; cursor at eol
        (when (= cursor-col (count line))
          [cursor cursor-type])

        suffix-text]]]
     children)))

(defn- input-placeholder [input-connr]
  (when-some [text (<sub [::subs/input-text input-connr])]
    [:> k/Text {:dim-color true
                :wrap :truncate-end}
     text]))

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
              {cursor-row :row cursor-col :col} (when focused?
                                                  (<sub [::buffer-subs/buffer-cursor bufnr]))
              input-focused? (<sub [::subs/input-focused? id])
              input-connr (<sub [::buffer-subs/->connr bufnr])
              last-row (:row (last lines))
              scrolled? (<sub [::subs/scrolled? id])]
          [:> k/Box {:flex-direction :column
                     :height :100%
                     :width :100%}
           [:> k/Box {:ref ref
                      :flex-direction :column
                      :flex-grow 1
                      :width :100%}
            (for [{:keys [row col line last-of-row?] :as data} lines]
              (let [input-line? (and (= last-row row)
                                     (not scrolled?)
                                     (some? input-connr))
                    inputting? (and input-line? input-focused?)]
                ^{:key [id row col]}
                [buffer-line
                 {:line data
                  :input-line? input-line?
                  :cursor-col (when (and (not inputting?)
                                         (= cursor-row row)
                                         (or last-of-row?
                                             (<= col cursor-col
                                                 (dec (+ col (count line))))))
                                cursor-col)

                  ; NOTE: It'd sure be nice if we could just pass this as a
                  ; child *but* for some reason when the cursor is on the line,
                  ; unless this extra text is part of the same k/Text tree, 3
                  ; chars get truncated from the buffer-line part... It's
                  ; definitely something to do with our cursor hack but I'm not
                  ; certain *what*, exactly.
                  :suffix-text (when (and input-line?
                                          (not inputting?))
                                 [input-placeholder input-connr])}
                 (when (and input-line? inputting?)
                   [input-window input-connr])]))]
           (cond
             (and scrolled? input-focused? input-connr)
             [input-window (<sub [::buffer-subs/->connr bufnr])]

             (and scrolled? input-connr)
             [input-placeholder input-connr]

             scrolled?
             [placeholders/line]

             ; NOTE: We *may* actually want to render something here to avoid the
             ; window size changing when we scroll... For now, though...
             ; it looks nicer without anything!
             :else nil)])))))
