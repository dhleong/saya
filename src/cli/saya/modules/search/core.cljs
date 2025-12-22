(ns saya.modules.search.core
  (:require
   [clojure.string :as str]
   [saya.modules.buffers.line :refer [->plain]]))

(defn- find-all [s f inc query]
  (let [length (count query)]
    (loop [offset nil
           results []]
      (let [idx (if offset
                  (f s query offset)
                  (f s query))
            next-idx (inc idx)]
        (cond
          (and (some? idx)
               (>= next-idx 0))
          (recur (inc idx)
                 (conj results {:at {:col idx}
                                :length length}))

          ; We found a match, but it's out of range, so can't be more
          (some? idx)
          (conj results {:at {:col idx}
                         :length length})

          ; Done!
          :else
          (seq results))))))

(defn in-plain-string [s direction query]
  (find-all
   s
   (case direction
     :newer str/index-of
     :older str/last-index-of)
   (case direction
     :newer inc
     :older dec)
   query))

(defn in-buffer [buffer direction query]
  (let [lines-count (count (:lines buffer))
        cursor (:cursor buffer)
        cursor-row (min lines-count
                        (:row cursor 0))

        ; NOTE: Doing reverse directly on :lines and constructing the
        ; line number in this way is more efficient since vec is reversible,
        ; so we don't have to materialize a whole collection directly
        vector-with-offset (case direction
                             :newer (fn [i line]
                                      [(+ cursor-row i) line])
                             :older (fn [i line]
                                      [(- cursor-row 1 i) line]))

        ; Filter the lines searched to the most relevant ones, very effeciently.
        ; If we want to support looping around, we could join two subvecs together
        relevant-lines (subvec
                        (:lines buffer)
                        (case direction
                          :newer cursor-row
                          :older 0)
                        (case direction
                          :newer lines-count
                          :older cursor-row))
        drop-while-cursor-compare (case direction
                                    :newer <=
                                    :older >=)]
    (->> (cond-> relevant-lines
           (= :older direction) (reverse))

         (into
          []
          (comp
           (map-indexed vector-with-offset)
           (mapcat (fn [[linenr line]]
                     ; TODO: Can we avoid the garbage of stripping ansi?
                     ; Is it worth it?
                     (-> (->plain line)
                         (in-plain-string direction query)
                         (->> (map (fn [match]
                                     (assoc-in match [:at :row] linenr)))))))
           (drop-while #(or (= (:at %)
                               cursor)
                            (and (= (:row (:at %))
                                    (:row cursor))
                                 (drop-while-cursor-compare
                                  (:col (:at %))
                                  (:col cursor))))))))))
