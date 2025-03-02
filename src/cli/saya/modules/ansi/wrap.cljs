(ns saya.modules.ansi.wrap
  (:require
   ["ansi-parser" :default AnsiParser]
   ["strip-ansi" :default strip-ansi]
   [clojure.string :as str]))

(defn- finalize-line [line]
  (.stringify AnsiParser (to-array line)))

(defn wrap-ansi [s width]
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
