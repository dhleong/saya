(ns saya.modules.search.core
  (:require
   ["strip-ansi" :default strip-ansi]
   [clojure.string :as str]
   [saya.modules.buffers.line :refer [->ansi]]))

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

(defn in-string [s direction query]
  ; TODO: Can we avoid the garbage of stripping ansi? Is it worth it?
  (find-all
   (strip-ansi s)
   (case direction
     :newer str/index-of
     :older str/last-index-of)
   (case direction
     :newer inc
     :older dec)
   query))

(defn in-buffer [buffer direction query]
  (let [lines-count (count (:lines buffer))
        cursor-row (min lines-count
                        (:row (:cursor buffer) 0))

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
                          :older cursor-row))]
    (->> (cond-> relevant-lines
           (= :older direction) (reverse))

         (map-indexed vector-with-offset)
         (mapcat (fn [[linenr line]]
                   (-> (->ansi line)
                       (in-string direction query)
                       (->> (map (fn [match]
                                   (assoc-in match [:at :row] linenr))))))))))
