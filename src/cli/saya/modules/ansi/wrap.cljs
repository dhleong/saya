(ns saya.modules.ansi.wrap
  (:require
   [applied-science.js-interop :as j]
   [clojure.string :as str]
   [saya.modules.ansi.split :refer [styled-chars->strings]]
   [taoensso.tufte :as tufte]))

(j/defn ^:private is-space? [^:js {:keys [value]}]
  (str/ends-with? value " "))

(defn- ->word-lengths [ansi-chars]
  (tufte/p
   ::word-lengths
   (->> ansi-chars
        (sequence
         (comp
          (partition-by is-space?)
          (remove #(is-space? (first %)))
          (map count))))))

(defn- conj-finished-line [dest finished-line]
  (conj dest (styled-chars->strings finished-line)))

(defn wrap-ansi-chars [ansi-chars width]
  {:pre [(number? width)]}
  (loop [lines []
         current-line []
         current-line-width 0
         ansi-chars ansi-chars
         word-lengths (->word-lengths ansi-chars)]

    (if (empty? ansi-chars)
      ; Done!
      (conj-finished-line lines current-line)

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
          (recur (conj-finished-line lines current-line)
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
