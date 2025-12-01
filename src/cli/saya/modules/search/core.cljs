(ns saya.modules.search.core
  (:require
   ["strip-ansi" :default strip-ansi]
   [clojure.string :as str]
   [saya.modules.buffers.line :refer [->ansi]]))

(defn in-string [s direction query]
  ; TODO: Can we avoid the garbage of stripping ansi? Is it worth it?
  (let [s (strip-ansi s)]
    (when-some [idx (case direction
                      :newer (str/index-of s query)
                      :older (str/last-index-of s query))]
      ; TODO: find all results in line + search across ansi
      [{:at {:col idx}
        :length (count query)}])))

(defn in-buffer [buffer direction query]
  ; TODO: "start at" cursor
  ; TODO: use the appropriate direction
  (let [results (->> (:lines buffer)
                     (map-indexed vector)
                     (mapcat (fn [[linenr line]]
                               (-> (->ansi line)
                                   (in-string direction query)
                                   (->> (map (fn [match]
                                               (assoc-in match [:at :row] linenr))))))))]
    (cond-> results
      (= :older direction) (reverse))))
