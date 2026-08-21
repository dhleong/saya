(ns saya.modules.buffers.line
  (:require
   ["ansi-parser" :default AnsiParser]
   ["strip-ansi" :default strip-ansi]
   [applied-science.js-interop :as j]
   [clojure.string :as str]
   [saya.modules.ansi.split :as split]
   [saya.modules.ansi.wrap :refer [wrap-ansi]]
   [taoensso.tufte :as tufte]))

(defprotocol IBufferLine
  (->ansi [this])
  (->plain [this])
  (ansi-chars [this])
  (length
    [this]
    "Visual length of this line in chars")
  (wrapped-lines [this width])
  (ansi-continuation
    [this]
    "The final ansi code state at the end of this line"))

(declare ->BufferLine BufferLine)

(defn- strip-unprintable [s]
  (str/replace s "\u0000" ""))

(defn- ->ansi-chars [parts]
  (->> parts
       (map (fn [{:keys [ansi system]}]
              (or system ansi)))
       (partition-by string?)
       (reduce
        (fn [formatted group]
          (concat
           formatted
           (if (string? (first group))
             (split/chars-with-ansi
              (str/join group))

             group)))
        [])))

(defn- part->plain [{:keys [ansi plain]}]
  (or plain
      (when ansi
        (strip-ansi ansi))))

(def ^:private EMPTY-PARTS [{:ansi "" :plain ""}])

(defn- compose-systems-onto-prior-strings []
  (fn [rf]
    (let [last-line (volatile! nil)
          feed-last-line (fn [result l]
                           (reduce rf result l))]
      (fn
        ([] (rf))
        ([result]
         (if-some [l @last-line]
           (rf (feed-last-line result l))
           (rf result)))
        ([result {:keys [strings systems]}]
         (let [l @last-line]
           (cond
             strings
             (let [result (if (some? l)
                            (feed-last-line result l)
                            result)]
               ; Store for later
               (vreset! last-line strings)
               result)

             (and systems (some? l))
             (let [with-system (vec l)
                   with-system (update with-system
                                       (dec (count with-system))
                                       into
                                       systems)]
               (vreset! last-line nil)
               (reduce rf result with-system))

             :else
             (rf result systems))))))))

(defn- drop-empty-lines-followed-by-others []
  (fn [rf]
    (let [last-line (volatile! nil)]
      (fn
        ([] (rf))
        ([result]
         (if-some [l @last-line]
           (rf (rf result l))
           (rf result)))
        ([result line]
         (let [l @last-line
               result (if (seq l)
                        (rf result l)
                        result)]
           (vreset! last-line line)
           result))))))

(defn- compose-col-numbers []
  ; Each subsequent chunk should have the appropriate :col
  ; value based on the length of the prior chunks
  (fn [rf]
    (let [last-line (volatile! nil)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result line]
         (let [l @last-line
               with-col {:col (+ (count (:line l))
                                 (:col l 0))
                         :line line}]
           (vreset! last-line with-col)
           (rf result with-col)))))))

(defn- ->wrapped-lines [parts width]
  (->> (or (seq parts)
           EMPTY-PARTS)

       (sequence
        (comp
         (map (fn [{:keys [ansi system]}]
                (or system ansi)))
         (partition-by string?)
         (map
          (fn [group]
            (if (string? (first group))
              {:strings (->> (wrap-ansi
                              (str/join group)
                              width)
                             (map split/chars-with-ansi))}

              {:systems group})))

       ; From above, `strings` will be a sequence of split lines,
       ; with each line being a sequence of chars-with-ansi;
       ; `systems` will be a sequence of system message vectors
       ; (basically, hiccup data).
       ; We know this was all meant to be a single line, so here
       ; we collapse the `systems` sequences *into* the preceeding
       ; line sequence (if any). This does mean that on a narrow
       ; screen a trailing system message could get clipped, but
       ; that's probably fine.
         (compose-systems-onto-prior-strings)

         ; When doing a hard split, we might end up with eg:
         ; [{:col 0 :line []}
         ;  {:col 0 :line ["-" "-" ..]}]
         ; This collapses that useless bit on the same
         ; column.
         (drop-empty-lines-followed-by-others)

         (compose-col-numbers)))))

(defn- perform-line-wrap [^BufferLine buffer-line width]
  (tufte/p
   ::perform-line-wrap
   (cond-> (->wrapped-lines (.-parts buffer-line) width)
     ; When profiling, it's easier to associate cost if we
     ; materialize upfront instead of staying lazy
     (seq js/process.env.PROFILE)
     (vec))
   #_[{:line (into (wrap-ansi (->ansi buffer-line) width)
                   (keep :system (.-parts buffer-line)))
       :col 0}]))

(defonce width-recalcs (atom 0))

(defn- cached-wrapped-lines [buffer-line width]
  (let [[for-width cached] (:wrapped @(.-state buffer-line))]
    (or (when (= for-width width)
          cached)
        (when for-width
          (swap! width-recalcs inc)
          nil)
        (-> (swap! (.-state buffer-line) assoc :wrapped [width (perform-line-wrap buffer-line width)])
            (:wrapped)
            (second)))))

(defn- ->ansi-continuation [ansi]
  (when-let [parts (seq (.parse AnsiParser (str ansi " ")))]
    (let [ansi (j/get (nth parts (dec (count parts))) :style)]
      (when (seq ansi)
        ansi))))

(defn- clean-part [o]
  (cond
    (string? o) {:ansi (strip-unprintable o)}

    (and (map? o)
         (:ansi o))
    (cond-> (update o :ansi strip-unprintable)
      (:plain o)
      (update :plain strip-unprintable))

    :else o))

(defn- concat->str [parts xducer]
  (transduce
   xducer
   str
   ""
   parts))

(deftype BufferLine [parts state]
  Object
  (equiv [this other]
    (-equiv this other))
  (toString [this]
    (->ansi this))

  IEquiv
  (-equiv [o other] (-equiv (.-parts o) (if (instance? BufferLine other)
                                          (.-parts other)
                                          other)))

  IHash
  (-hash [_] (-hash parts))

  ICollection
  (-conj [this o]
    (->BufferLine (conj (.-parts this) (clean-part o)) (atom nil)))

  ICounted
  (-count [this]
    (count (.-parts this)))

  ISequential
  ISeqable
  (-seq [this]
    (seq (.-parts this)))

  IBufferLine
  (->ansi [_]
    (or (:ansi @state)
        (:ansi (swap! state assoc :ansi (->> (keep :ansi parts)
                                             (str/join))))))

  (->plain [_]
    (or (:plain @state)
        (:plain (swap! state assoc :plain (->> (keep part->plain parts)
                                               (str/join))))))

  (ansi-chars [_]
    (or (:chars @state)
        (:chars (swap! state assoc :chars (->ansi-chars parts)))))

  (length [this]
    (count (ansi-chars this)))

  (wrapped-lines [this width]
    ; TODO: This should probably be some kind of LRU cache when we support
    ; splitting windows...
    (tufte/p
     ::cached-wrapped-lines
     (cached-wrapped-lines this width)))

  (ansi-continuation [this]
    ; NOTE: We don't cache this because we *shouldn't* need it
    ; more than once per line anyway
    (->ansi-continuation (->ansi this))))

(extend-protocol IPrintWithWriter
  BufferLine
  (-pr-writer [a writer opts]
    (-write writer "#BufferLine[")
    (if (some :system (.-parts a))
      (do
        (-write writer "[")
        (pr-seq-writer (.-parts a) writer opts)
        (-write writer "]"))

      (do
        (-write writer "\"")
        (-write writer (->ansi a))
        (-write writer "\"")))
    (-write writer "]")))

(def EMPTY (->BufferLine [] (atom nil)))

(defn buffer-line
  ([] EMPTY)
  ([initial-part]
   (if (some? initial-part)
     (->BufferLine
      [(clean-part initial-part)]
      (atom nil))
     EMPTY)))

(comment
  ; Clear all line caches
  #_{:clj-kondo/ignore [:unresolved-namespace]}
  (doseq [[_ buffer] (:buffers @re-frame.db/app-db)]
    (doseq [line (:lines buffer)]
      (reset! (.-state line) nil))))
