(ns saya.modules.ansi.wrap
  (:require
   ["ansi-parser" :default AnsiParser]
   [clojure.string :as str]
   [taoensso.tufte :as tufte]))

(defn- trim-suffix [s suffix]
  (cond-> s
    (str/ends-with? s suffix) (subs 0 (- (count s)
                                         (count suffix)))))

(defn- finalize-line [line]
  (tufte/p
   ::finalize-line-parser
   (-> (.stringify AnsiParser (to-array line))
          ; This trailing "reset styles" is "nice" but unnecessary.
          ; ink should handle it for us, so keeping it is just noise
       (trim-suffix "\u001B[0m"))))

(defn- is-space? [part]
  (= " " (.-content part)))

(defn- ->ansi-chars [^String s]
  (tufte/p
   ::ansi-chars
   (seq (.parse AnsiParser s))))

(defn- ->word-lengths [ansi-chars]
  (tufte/p
   ::word-lengths
   (->> ansi-chars
        (sequence
         (comp
          (partition-by is-space?)
          (remove #(is-space? (first %)))
          (map count))))))

(defn- wrap-ansi-chars-internal [finalize ansi-chars width]
  {:pre [(number? width)]}
  (loop [lines []
         current-line []
         current-line-width 0
         ansi-chars ansi-chars
         word-lengths (->word-lengths ansi-chars)]

    (if (empty? ansi-chars)
      ; Done!
      (conj lines (finalize current-line))

      (let [word-len (first word-lengths)
            want-to-take (inc word-len)
            to-take (min width want-to-take)
            next-word-lengths (if (> want-to-take to-take)
                                ; had to hard split a word (uncommon)
                                (cons (- want-to-take to-take)
                                      (next word-lengths))
                                (next word-lengths))]
        (if (> (+ current-line-width word-len 1)
               width)
          ; Wrap
          (recur (conj lines (finalize current-line))
                 (into [] (take to-take ansi-chars))
                 ; (to-array (take to-take ansi-chars))
                 to-take ; new line initial length
                 (drop to-take ansi-chars)
                 next-word-lengths)

          ; Continue on line
          (recur lines
                 ; (into current-line (take to-take ansi-chars))
                 (into current-line (take to-take ansi-chars))
                 (+ current-line-width to-take)
                 (drop to-take ansi-chars)
                 next-word-lengths))))))

(defn wrap-ansi-chars [ansi-chars width]
  (wrap-ansi-chars-internal identity ansi-chars width))

(defn wrap-ansi [s width]
  (tufte/p
   ::wrap-ansi
   (wrap-ansi-chars-internal finalize-line (->ansi-chars s) width)))
