(ns saya.modules.buffers.line
  (:require
   ["ansi-parser" :default AnsiParser]
   ["wrap-ansi" :default wrap-ansi]
   [applied-science.js-interop :as j]
   [clojure.string :as str]
   [saya.modules.ansi.split :as split]))

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

(defn- ->wrapped-lines [parts width]
  (->> parts
       (map (fn [{:keys [ansi system]}]
              (or system ansi)))
       (partition-by string?)

       (mapcat
        (fn [group]
          (if (string? (first group))
            (->> (wrap-ansi
                  (apply str group)
                  width
                  #js {:trim false
                       :hard true
                       :wordWrap true})
                 (str/split-lines)
                 (map split/chars-with-ansi))

            [group])))

       (reduce
        (fn [result line]
          (conj result {:col (+ (count (:line (peek result)))
                                (:col (peek result) 0))
                        :line line}))
        [])))

(defn- ->ansi-continuation [ansi]
  (when-let [parts (seq (.parse AnsiParser (str ansi " ")))]
    (let [ansi (j/get (nth parts (dec (count parts))) :style)]
      (when (seq ansi)
        ansi))))

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
    (->BufferLine (conj (.-parts this) o) (atom nil)))

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
        (:ansi (swap! state assoc :ansi (apply str (map :ansi parts))))))

  (ansi-chars [_]
    (or (:chars @state)
        (:chars (swap! state assoc :chars (->ansi-chars parts)))))

  (length [this]
    (count (ansi-chars this)))

  (wrapped-lines [_ width]
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
      [(if (string? initial-part)
         {:ansi initial-part}
         initial-part)]
      (atom nil))
     EMPTY)))
