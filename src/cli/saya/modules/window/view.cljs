(ns saya.modules.window.view
  (:require
   ["ink" :as k]
   ["react" :as React]
   ["strip-ansi" :default strip-ansi]
   [applied-science.js-interop :as j]
   [archetype.util :refer [<sub >evt]]
   [saya.cli.text-input.helpers :refer [split-text-by-state]]
   [saya.config :as config]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.buffers.subs :as buffer-subs]
   [saya.modules.connection.completion :refer [->ConnectionCompletionSource]]
   [saya.modules.connection.events :as conn-events]
   [saya.modules.input.window :as input-window]
   [saya.modules.perf.core :as perf]
   [saya.modules.search.subs :as search-subs]
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
                   (when (seq js/process.env.REPLAY_DUMP)
                     (perf/use-tti-end-effect :replay/connection))
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
  (let [bufnr [:conn/input connr]]
    [input-window/input-window
     {:bufnr bufnr
      :initial-value (<sub [::subs/input-text connr])
      :initial-cursor (:col (<sub [::buffer-subs/buffer-cursor bufnr]))
      :completion (->ConnectionCompletionSource connr)
      :add-to-history? false ; Handled by submit-input-buffer
      :on-persist-value #(>evt [::window-events/set-input-text {:connr connr
                                                                :text %}])
      :on-persist-cursor (fn [col]
                           (>evt [::buffer-events/set-cursor
                                  {:id bufnr
                                   :cursor {:row 0 :col col}}]))
      :on-prepare-buffer #(>evt [::window-events/prepare-input-cmdline-buffer
                                 {:bufnr bufnr
                                  :current %}])
      :on-submit (fn [text]
                   (>evt [::conn-events/submit-input-buffer
                          {:connr connr
                           :text text}]))}]))

(defn- modeful-cursor []
  (let [cursor-type (case (<sub [:mode])
                      :insert :pipe
                      :operator-pending :underscore
                      :block)]
    [cursor cursor-type]))

(defn- highlighted? [highlights col]
  ; Start with a quick reject so we don't check every match for every col
  (when (and (seq highlights)
             (>= col (:col (first highlights))))
    (some
     (fn [{:keys [at length]}]
       (<= (:col at) col (dec (+ (:col at) length))))
     highlights)))

(defn- buffer-line [{:keys [cursor-col input-line? suffix-text highlights]
                     {:keys [line col]} :line}
                    & children]
  (let [; We should at least render a blank column, in case
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
                 :position :relative
                 ; :width (cond
                 ;          (and cursor-col input-line?) 48
                 ;          (not input-line?) :100%)
                 :width (when-not input-line?
                          :100%)}

       [:> k/Text {:wrap :truncate-end}
        (for [[i part] (map-indexed vector line)]
          (let [abs-col (+ col i)]
            ^{:key i}
            [:<>
             (when (= cursor-col abs-col)
               [modeful-cursor])
             (if (vector? part)
               (into [(system-messages (first part))] (rest part))
               [:> k/Text (when (highlighted? highlights abs-col)
                            {:background-color :yellow})
                (cond-> part
                  config/no-ansi? (strip-ansi))])]))

        ; cursor at eol
        (when (= cursor-col (count line))
          [modeful-cursor])

        suffix-text]]]

     children)))

(defn- input-placeholder [input-connr]
  (let [current-window (<sub [:current-window])
        conn-pending-operator? (<sub [::subs/conn-pending-operator?])
        current-buffer (<sub [:current-buffer])
        input-text (<sub [::subs/input-text input-connr])
        mode (<sub [:mode])]
    (when-not (and (= :cmdline (:id current-window))
                   (= [:conn/input input-connr] (:bufnr current-window)))
      (cond
        (or (= :prompt mode)
            (and conn-pending-operator?
                 (= input-connr
                    (:connection-id current-buffer))))
        (let [input-buffer (<sub [::buffer-subs/by-id [:conn/input input-connr]])
              [before after] (split-text-by-state
                              input-buffer
                              input-text)]
          [:> k/Text
           before
           [modeful-cursor]
           after])

        ; Normal mode placeholder
        (seq input-text)
        [:> k/Text {:dim-color true
                    :wrap :truncate-end}
         input-text]))))

(defn- conn-single-prompt [input-connr]
  (when-some [single-prompt (<sub [::subs/single-prompt input-connr])]
    [:> k/Text single-prompt]))

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
              conn-pending-operator? (when focused?
                                       (<sub [::subs/conn-pending-operator?]))
              {cursor-row :row cursor-col :col} (when focused?
                                                  (<sub [::buffer-subs/current-buffer-cursor bufnr]))
              input-focused? (<sub [::subs/input-focused? id])
              input-connr (<sub [::buffer-subs/->connr bufnr])
              last-row (:row (last lines))
              scrolled? (<sub [::subs/scrolled? id])
              search-results (<sub [::search-subs/results-by-line {:bufnr bufnr
                                                                   :start (:row (first lines))
                                                                   :end last-row}])]
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
                                         (not conn-pending-operator?)
                                         (or last-of-row?
                                             (<= col cursor-col
                                                 (dec (+ col (count line))))))
                                cursor-col)

                  :highlights (get search-results row)

                  ; NOTE: It'd sure be nice if we could just pass this as a
                  ; child *but* for some reason when the cursor is on the line,
                  ; unless this extra text is part of the same k/Text tree, 3
                  ; chars get truncated from the buffer-line part... It's
                  ; definitely something to do with our cursor hack but I'm not
                  ; certain *what*, exactly.
                  :suffix-text [:<>
                                ; NOTE: This *should* be relatively safe
                                ; due to the way Kodachi clears the
                                ; "partial" line to handle prompts
                                (when (and input-line? (empty? line))
                                  [conn-single-prompt input-connr])
                                (when (and input-line?
                                           (not inputting?))
                                  [input-placeholder input-connr])]}

                 (when (and input-line? inputting?)
                   [input-window input-connr])]))]
           [:> k/Box {:flex-direction :row
                      :flex-wrap :wrap}
            (when scrolled?
              [conn-single-prompt input-connr])
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
              :else nil)]])))))
