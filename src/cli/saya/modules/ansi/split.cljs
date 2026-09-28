(ns saya.modules.ansi.split
  (:require
   ["@alcalzone/ansi-tokenize" :as ansi]
   [applied-science.js-interop :as j]))

(defn ->ansi-tokens [^String s]
  (ansi/tokenize s))

(defn tokens->styled-chars [toks]
  (concat
   (->> toks
        (take-while (complement vector?))
        (ansi/styledCharsFromTokens))
   (drop-while (complement vector?) toks)))

(defn styled-chars->strings [tokenized-chars]
  (->> tokenized-chars
       (remove vector?)
       (reduce
        (j/fn [[last-styles rv] ^:js {:keys [value styles]}]
          (let [diff (ansi/diffAnsiCodes last-styles styles)
                new-ansi (ansi/ansiCodesToString diff)
                output (if (seq new-ansi)
                         (str new-ansi value)
                         value)]
            [styles (conj rv output)]))
        [#js [] []])
       peek))

(defn styled-chars->trailing-ansi [styled-chars]
  (when-some [^js last-char (if (vector? styled-chars)
                              (peek styled-chars)
                              (last styled-chars))]
    (ansi/ansiCodesToString (.-styles last-char))))
