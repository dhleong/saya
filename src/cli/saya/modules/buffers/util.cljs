(ns saya.modules.buffers.util
  (:require
   [saya.modules.input.insert :refer [line->string]]))

(defn readonly? [buffer]
  (some?
   (:readonly
    (:flags buffer))))

(defn char-at [{:keys [lines]} {:keys [row col]}]
  {:pre [(>= row 0)
         (>= col 0)]}
  (when (<= 0 row (dec (count lines)))
    (let [line (line->string (nth lines row))]
      (when (<= 0 col (dec (count line)))
        (.charAt line col)))))

(defn update-cursor [{:keys [lines] :as buffer} cursor pred]
  (let [cursor' (update cursor :col pred)]
    (if-not (nil? (char-at buffer cursor))
      cursor'

      (let [cursor' (update cursor :row pred)]
        (cond
          (> (:row cursor')
             (:row cursor))
          (assoc cursor' :col 0)

          (< (:row cursor) 0)
          {:row 0 :col 0}

          ; Past the end of the buffer; don't move!
          (>= (:row cursor) (count lines))
          cursor

          :else
          (assoc cursor' :col
                 (dec (count (nth lines (:row cursor))))))))))
