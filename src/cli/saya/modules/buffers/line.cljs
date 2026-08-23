(ns saya.modules.buffers.line
  (:require
   ["strip-ansi" :default strip-ansi]
   [clojure.string :as str]
   [saya.modules.ansi.split :as split]
   [saya.modules.ansi.wrap :refer [wrap-ansi-chars]]
   [taoensso.tufte :as tufte]))

(defprotocol IBufferLine
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

(def ^:private EMPTY-PARTS [{:ansi "" :plain ""}])

(defn- strip-unprintable [s]
  (str/replace s "\u0000" ""))

(defn- tokenized-parts [^BufferLine buffer-line]
  (or (:tokens @(.-state buffer-line))
      (:tokens (swap! (.-state buffer-line)
                      assoc
                      :tokens
                      (->> (or (.-parts buffer-line)
                               EMPTY-PARTS)
                           (sequence
                            (comp
                             (map (fn [{:keys [ansi system]}]
                                    (or system ansi)))
                             (partition-by string?)
                             (mapcat
                              (fn [group]
                                (if (string? (first group))
                                  (mapcat split/->ansi-tokens
                                          group)
                                  group))))))))))

(defn- part->plain [{:keys [ansi plain]}]
  (or plain
      (when ansi
        (strip-ansi ansi))))

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

(defn- ->wrapped-lines [^BufferLine buffer-line width]
  (->> (or (seq (tokenized-parts buffer-line))
           [""])

       (sequence
        (comp
         (partition-by string?)
         (map
          (fn [group]
            (if (vector? (first group))
              {:systems group}

              {:strings
               (wrap-ansi-chars
                (split/tokens->chars-with-ansi group)
                width)})))

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
   (cond-> (->wrapped-lines buffer-line width)
     ; When profiling, it's easier to associate cost if we
     ; materialize upfront instead of staying lazy
     (seq js/process.env.PROFILE)
     (vec))))

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

(defn- ->ansi-continuation [^BufferLine buffer-line]
  (let [tokens (->> (tokenized-parts buffer-line)
                    (take-while (complement vector?)))
        last-tok (last tokens)]
    ; NOTE: the last ^here and below operate on sequences.
    ; Not the most efficient, but it's a simple way to ensure
    ; system messages don't interfere
    (or (when (and last-tok
                   (= "ansi" (.-type last-tok)))
          (if (= "\u001B[0m" (.-code last-tok))
            "" ; Hacks...?
            (.-code last-tok)))
        (let [parts (split/tokens->chars-with-ansi tokens)]
          (when-some [last-char (last parts)]
            (subs last-char 0 (dec (count last-char))))))))

(defn- clean-part [o]
  (cond
    (string? o) {:ansi (strip-unprintable o)}

    (and (map? o)
         (:ansi o))
    (cond-> (update o :ansi strip-unprintable)
      (:plain o)
      (update :plain strip-unprintable))

    :else o))

(deftype BufferLine [parts state]
  Object
  (equiv [this other]
    (-equiv this other))
  (toString [_]
    (->> (keep :ansi parts)
         (str/join)))

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

  (->plain [_]
    (or (:plain @state)
        (:plain (swap! state assoc :plain (->> (keep part->plain parts)
                                               (str/join))))))

  (ansi-chars [this]
    (split/tokens->chars-with-ansi
     (tokenized-parts this)))

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
    (->ansi-continuation this)))

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
        (-write writer (str a))
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
