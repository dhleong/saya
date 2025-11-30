(ns saya.modules.buffers.line
  (:require
   ["ansi-parser" :default AnsiParser]
   [applied-science.js-interop :as j]
   [clojure.string :as str]
   [saya.modules.ansi.split :as split]
   [saya.modules.ansi.wrap :refer [wrap-ansi]]))

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
              (apply str group))

             group)))
        [])))

(def ^:private EMPTY-PARTS [{:ansi ""}])

(defn- ->wrapped-lines [parts width]
  (->> (or (seq parts)
           EMPTY-PARTS)
       (map (fn [{:keys [ansi system]}]
              (or system ansi)))
       (partition-by string?)

       (map
        (fn [group]
          (if (string? (first group))
            {:strings (->> (wrap-ansi
                            (apply str group)
                            width)
                           (map split/chars-with-ansi))}

            {:systems group})))

       ; NOTE: These double vector is a bit itchy... can we do better?

       ; From above, `strings` will be a sequence of split lines,
       ; with each line being a sequence of chars-with-ansi;
       ; `systems` will be a sequence of system message vectors
       ; (basically, hiccup data).
       ; We know this was all meant to be a single line, so here
       ; we collapse the `systems` sequences *into* the preceeding
       ; line sequence (if any). This does mean that on a narrow
       ; screen a trailing system message could get clipped, but
       ; that's probably fine.
       (reduce
        (fn [result {:keys [strings systems]}]
          (if strings
            (into result strings)
            (conj (if (seq result)
                    (pop result)
                    result)
                  (concat (peek result) systems))))
        [])

       (reduce
        (fn [result line]
          (let [last-line (peek result)]
            (cond-> result
              ; When doing a hard split, we might end up with eg:
              ; [{:col 0 :line []}
              ;  {:col 0 :line ["-" "-" ..]}]
              ; This collapses that useless bit on the same
              ; column.
              ; TODO: would be better to fix upstream, but this
              ; prevents react complaining about duplicate keys
              ; until then.
              (and last-line
                   (empty? (:line last-line)))
              (pop)

              :always
              (conj {:col (+ (count (:line (peek result)))
                             (:col (peek result) 0))
                     :line line}))))
        [])))

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
    (update o :ansi strip-unprintable)

    :else o))

(defprotocol IBufferLine
  (->ansi [this])
  (ansi-chars [this])
  (length
    [this]
    "Visual length of this line in chars")
  (wrapped-lines [this width])
  (ansi-continuation
    [this]
    "The final ansi code state at the end of this line"))

(declare ->BufferLine)

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
        (:ansi (swap! state assoc :ansi (->> (map :ansi parts)
                                             (apply str))))))

  (ansi-chars [_]
    (or (:chars @state)
        (:chars (swap! state assoc :chars (->ansi-chars parts)))))

  (length [this]
    (count (ansi-chars this)))

  (wrapped-lines [_ width]
    ; TODO: This should probably be some kind of LRU cache when we support
    ; splitting windows...
    (let [[for-width cached] (:wrapped @state)]
      (or (when (= for-width width)
            cached)
          (-> (swap! state assoc :wrapped [width (->wrapped-lines parts width)])
              (:wrapped)
              (second)))))

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
