(ns saya.cli.text-input
  (:require
   ["ink" :as k]
   [clojure.core.match :refer [match]]
   [clojure.string :as str]
   [reagent.core :as r]
   [saya.cli.input :refer [use-keys]]
   [saya.modules.completion.helpers :refer [word-to-complete]]
   [saya.modules.ui.cursor :refer [cursor]]))

(defn- split-text-by-state [{:keys [cursor]} value]
  [(subs value 0 cursor)
   (subs value cursor)])

(defn- dec-to-zero [v]
  (cond-> v
    (> v 0) (dec)))

(defn- inc-to-max [v max-value]
  (min (inc v) max-value))

(defn- try-update-completion [state {:keys [word] :as new-state}]
  (when word
    (if (some? (:index state))
      ; If there's a non-nil index, we're cycling through candidates
      state
      new-state)))

(defn- clamp-to-candidates [candidates idx]
  (when-not (or (>= idx (count candidates))
                (< idx 0))
    idx))

(defn next-candidate [state index-pred value completion]
  (let [state' (-> state
                   (update :completion try-update-completion completion))
        candidates (get-in state' [:completion :candidates])]
    (if (seq candidates)
      (let [state' (update-in state' [:completion :index]
                              (comp
                               (partial clamp-to-candidates candidates)
                               index-pred))
            new-candidate (if-some [n (:index (:completion state'))]
                            (nth candidates n)
                            ; Replace with the original input
                            (:word (:completion state')))

            [before after] (split-text-by-state state' value)
            completed-before (word-to-complete {:line-before-cursor before})
            before' (subs before 0 (- (count before)
                                      (count completed-before)))
            result (str before'
                        new-candidate
                        after)]
        (-> state'
            (assoc :cursor (+ (count before') (count new-candidate)))
            (assoc-in [:completion :result] result)))

      state')))

(defn- cycle-completion-candidates [{:keys [on-change completion-candidates completion-word]}
                                    state-ref index-pred value]
  (let [new-completion {:word completion-word :candidates completion-candidates}
        [_ new-state] (swap-vals! state-ref next-candidate index-pred value new-completion)]
    ; Storing this result in the state like this is pretty gross:
    (when-let [value' (:result (:completion new-state))]
      (on-change value' (:cursor new-state)
                 {:applied-candidate (when-some [idx (:index (:completion new-state))]
                                       (nth (:candidates (:completion new-state))
                                            idx))}))))

(defn- on-key [{:keys [completion-candidates _completion-word
                       on-change on-key on-submit]
                :as params}
               state-ref value key]
  (match [key]
    [:return] (on-submit value)
    [:delete] (let [[_ new-state] (swap-vals! state-ref update :cursor dec-to-zero)
                    [before after] (split-text-by-state new-state value)
                    new-value (str before (subs after 1))]
                (on-change new-value (:cursor new-state)))

    [:ctrl/a] (swap! state-ref assoc :cursor 0)
    [:ctrl/e] (swap! state-ref assoc :cursor (count value))

    [:left] (swap! state-ref update :cursor dec-to-zero)
    [:right] (swap! state-ref update :cursor inc-to-max (count value))

    ; TODO: Reuse logic for "delete back word" from regular mappings
    [:meta/delete] (let [{:keys [cursor]} @state-ref
                         last-word-start (or (str/last-index-of value " " cursor) 0)]
                     (swap! state-ref assoc :cursor last-word-start)
                     (on-change (str (subs value 0 last-word-start)
                                     (subs value cursor))
                                last-word-start))

    [:tab] (cycle-completion-candidates params state-ref (fnil inc -1) value)
    [:shift/tab] (cycle-completion-candidates
                  params state-ref
                  (fnil dec (count completion-candidates))
                  value)

    [(key :guard string?)] (let [[old-state {:keys [cursor]}] (swap-vals! state-ref
                                                                          update
                                                                          :cursor + (count key))
                                 [before after] (split-text-by-state old-state value)
                                 new-value (str before key after)]
                             (on-change new-value cursor))

    :else (on-key key)))

(defn text-input [{:keys [value _ghost _completion-candidates _completion-word
                          _on-change _on-key _on-submit] :as params
                   cursor-shape :cursor}]
  (r/with-let [state-ref (r/atom {:cursor 0})]
    (use-keys :text-input (partial on-key params state-ref value))

    (let [[before after] (split-text-by-state @state-ref value)]
      [:> k/Text
       [:> k/Text before]
       [cursor (or cursor-shape :block)]
       ; NOTE: ghost is *cool* but the delay between re-frame updating
       ; the ghost and react updating the text is awful. Perhaps if we
       ; moved to storing state in the DB and using `dispatch-sync` we
       ; could do this...
       ; (when (seq ghost)
       ;   [:> k/Text {:dim-color true} ghost])
       [:> k/Text after]])))
