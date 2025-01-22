(ns saya.modules.ui.cursor
  (:require
   ["ink" :as k]
   ["react" :as React]
   [clojure.string :as str]))

(defonce ^:private shape-ref (atom :block))

; We use a couple zero-width characters that are highly unlikely
; to actually be used together:
; - Zero-width space
; - Zero-width non-joiner (should be between characters that normally
;   are rendered together with ligatures)
; - Another zero-width space
(def ^:private cursor-text "\u200B\u200C\u200B")

(defn extract-cursor-position [lines]
  (loop [y 0
         lines lines]
    (when-some [line (first lines)]
      (if-let [x (str/index-of line cursor-text)]
        {:x x :y y}
        (recur (inc y)
               (next lines))))))

(defn get-cursor-shape []
  (or @shape-ref :block))

(defn strip-cursor
  "Trying to actually render our cursor text can cause lines
   to break in ways that the renderer can't diff correctly, so
   we just strip it out before rendering."
  [s]
  (str/replace s cursor-text ""))

(defn- f>cursor [shape]
  (React/useEffect
   (fn []
     (reset! shape-ref shape)

     js/undefined))
  [:> k/Text cursor-text])

(defn cursor
  ([] [cursor :block])
  ([shape] [:f> f>cursor shape]))
