(ns saya.util.coll)

(defn split-when
  "Split a collection on the first truthy value of `(pred element)`.
  Returns `[before element after]`, where `element` will be `not-found`
  (defaulting to nil) if `(pred element)` never returned truthy. Additionally,
  if it never returned truthy, all elements will be in `after`, and `before`
  will be nil.

  If returned, `before` will be a vector
  "
  ([pred coll] (split-when pred nil coll))
  ([pred not-found coll]
   (loop [before []
          coll coll]
     (if-let [current (first coll)]
       (if (pred current)
         [before current (next coll)]
         (recur (conj before current)
                (next coll)))

       [nil not-found before]))))
