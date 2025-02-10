(ns saya.cli.text-input.helpers)

(defn dec-to-zero [v]
  (cond-> v
    (> v 0) (dec)))

(defn inc-to-max [v max-value]
  (min (inc v) max-value))

(defn split-text-by-state [{:keys [cursor]} value]
  (let [idx (if (number? cursor)
              cursor
              (:col cursor))]
    [(subs value 0 idx)
     (subs value idx)]))

