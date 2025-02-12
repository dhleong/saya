(ns saya.modules.buffers.util)

(defn readonly? [buffer]
  (some?
   (:readonly
    (:flags buffer))))

