(ns saya.modules.buffers.line
  (:require
   ["strip-ansi" :default strip-ansi]
   ["wrap-ansi" :default wrap-ansi]
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

(defprotocol IBufferLine
  (->ansi [this])
  (->plain [this])
  (ansi-chars [this])
  (wrapped-lines [this width]))

(declare ->BufferLine)

(deftype BufferLine [parts state]
  Object
  (equiv [this other]
    (-equiv this other))

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

  (->plain [this]
    (or (:plain @state)
        (:plain (swap! state assoc :plain (strip-ansi
                                           (->ansi this))))))

  (ansi-chars [_]
    (or (:chars @state)
        (:chars (swap! state assoc :chars (->ansi-chars parts)))))

  (wrapped-lines [this width]
    (let [[for-width cached] (:wrapped @state)]
      (or (when (= for-width width)
            cached)
          (-> (swap! state assoc :wrapped [width (-> (->ansi this)
                                                     (wrap-ansi
                                                      width
                                                      #js {:trim false
                                                           :hard true
                                                           :wordWrap true})
                                                     (str/split-lines))])
              (:wrapped)
              (second))))))

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
   (->BufferLine [initial-part] (atom nil))))
