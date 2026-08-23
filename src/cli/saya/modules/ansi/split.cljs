(ns saya.modules.ansi.split
  (:require
   ["@alcalzone/ansi-tokenize" :as ansi]
   [applied-science.js-interop :as j]))

(defn ->ansi-tokens [^String s]
  (ansi/tokenize s))

(defn tokens->chars-with-ansi [toks]
  (concat
   (->> toks
        (take-while (complement vector?))
        (ansi/styledCharsFromTokens)
        (map (j/fn [^:js {:keys [value styles]}]
               (str (ansi/ansiCodesToString styles)
                    value))))
   (drop-while (complement vector?) toks)))
