(ns saya.modules.buffers.util
  (:require
   [saya.modules.buffers.line :refer [length]]
   [saya.modules.input.insert :refer [line->string]]))

(defn readonly? [buffer]
  (some?
   (:readonly
    (:flags buffer))))

(defn line-length [{:keys [lines]} row]
  (->> (nth lines row)
       (length)))

(defn char-at
  ([{:keys [cursor] :as buffer}] (char-at buffer cursor))
  ([{:keys [lines]} {:keys [row col]}]
   (when (<= 0 row (dec (count lines)))
     (let [line (line->string (nth lines row))]
       (when (<= 0 col (dec (count line)))
         (.charAt line col))))))

(defn update-cursor
  ([{:keys [cursor] :as buffer} pred]
   (assoc buffer :cursor (update-cursor buffer cursor pred)))
  ([{:keys [lines] :as buffer} cursor pred]
   (let [cursor' (update cursor :col pred)]
     (if-not (nil? (char-at buffer cursor'))
       cursor'

       (let [cursor' (update cursor :row pred)]
         (cond
           ; Past the end of the buffer; don't move!
           (>= (:row cursor') (count lines))
           cursor

           ; Past the start of the buffer; stay at the start!
           (< (:row cursor') 0)
           {:row 0 :col 0}

           ; Next line
           (> (:row cursor')
              (:row cursor))
           (assoc cursor' :col 0)

           ; Previous line
           :else
           (assoc cursor' :col
                  (dec (line-length buffer (:row cursor'))))))))))
