(ns saya.util.functional)

(defn with-meta->>
  "A convenience variant of with-meta suitable for use in a ->> call site"
  [m o]
  (with-meta o m))

