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
  ; TODO: "start at" cursor
  (let [results (->> (:lines buffer)
                     (map-indexed vector)
                     (mapcat (fn [[linenr line]]
                               (-> (->ansi line)
                                   (in-string direction query)
                                   (->> (map (fn [match]
                                               (assoc-in match [:at :row] linenr))))))))]
    (cond-> results
      (= :older direction) (reverse))))
