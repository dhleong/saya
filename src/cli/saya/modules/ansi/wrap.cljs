(ns saya.modules.ansi.wrap
  (:require
   [clojure.string :as str]
   [taoensso.tufte :as tufte]))

(defn trim-suffix [s suffix]
  (cond-> s
    (str/ends-with? s suffix) (subs 0 (- (count s)
                                         (count suffix)))))

(defn- is-space? [part]
  (str/ends-with? part " "))

(defn- ->word-lengths [ansi-chars]
  (tufte/p
   ::word-lengths
   (->> ansi-chars
        (sequence
         (comp
          (partition-by is-space?)
          (remove #(is-space? (first %)))
          (map count))))))

(defn wrap-ansi-chars [ansi-chars width]
  {:pre [(number? width)]}
  (loop [lines []
         current-line []
         current-line-width 0
         ansi-chars ansi-chars
         word-lengths (->word-lengths ansi-chars)]

    (if (empty? ansi-chars)
      ; Done!
      (conj lines current-line)

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
          (recur (conj lines current-line)
                 (into [] (take to-take ansi-chars))
                 to-take ; new line initial length
                 (drop to-take ansi-chars)
                 next-word-lengths)

          ; Continue on line
          (recur lines
                 (into current-line (take to-take ansi-chars))
                 (+ current-line-width to-take)
                 (drop to-take ansi-chars)
                 next-word-lengths))))))
