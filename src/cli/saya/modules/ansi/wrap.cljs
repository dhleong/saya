(ns saya.modules.ansi.wrap
  (:require
   ["ansi-parser" :default AnsiParser]
   ["strip-ansi" :default strip-ansi]
   [clojure.string :as str]))

(defn- trim-suffix [s suffix]
  (cond-> s
    (str/ends-with? s suffix) (subs 0 (- (count s)
                                         (count suffix)))))

(defn- finalize-line [line]
  (-> (.stringify AnsiParser (to-array line))
      ; This trailing "reset styles" is "nice" but unnecessary.
      ; ink should handle it for us, so keeping it is just noise
      (trim-suffix "\u001B[0m")))

(defn wrap-ansi [s width]
  {:pre [(number? width)]}
  (loop [lines []
         current-line []
         current-line-width 0
         word-lengths (mapv count
                            (str/split (strip-ansi s) #" "))
         ansi-chars (seq (.parse AnsiParser s))]
    (if (empty? ansi-chars)
      ; Done!
      (conj lines (finalize-line current-line))

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
          (recur (conj lines (finalize-line current-line))
                 (into [] (take to-take ansi-chars))
                 to-take ; new line initial length
                 next-word-lengths
                 (drop to-take ansi-chars))

          ; Continue on line
          (recur lines
                 (into current-line (take to-take ansi-chars))
                 (+ current-line-width to-take)
                 next-word-lengths
                 (drop to-take ansi-chars)))))))
