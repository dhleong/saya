(ns saya.cli.text-input
  (:require
   ["ink" :as k]
   [clojure.core.match :refer [match]]
   [clojure.string :as str]
   [reagent.core :as r]
   [saya.cli.input :refer [use-keys]]
   [saya.modules.ui.cursor :refer [cursor]]))

(defn- split-text-by-state [{:keys [cursor]} value]
  [(subs value 0 cursor)
   (subs value cursor)])

(defn- dec-to-zero [v]
  (cond-> v
    (> v 0) (dec)))

(defn- inc-to-max [v max-value]
  (min (inc v) max-value))

(defn- on-key [{:keys [on-change on-key on-submit]} state-ref value key]
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

    [(key :guard string?)] (let [[old-state {:keys [cursor]}] (swap-vals! state-ref
                                                                          update
                                                                          :cursor + (count key))
                                 [before after] (split-text-by-state old-state value)
                                 new-value (str before key after)]
                             (on-change new-value cursor))

    :else (on-key key)))

(defn text-input [{:keys [value _on-change _on-key _on-submit] :as params
                   cursor-shape :cursor}]
  (r/with-let [state-ref (r/atom {:cursor 0})]
    (use-keys :text-input (partial on-key params state-ref value))

    (let [[before after] (split-text-by-state @state-ref value)]
      [:> k/Text
       [:> k/Text before]
       [cursor (or cursor-shape :block)]
       [:> k/Text after]])))
