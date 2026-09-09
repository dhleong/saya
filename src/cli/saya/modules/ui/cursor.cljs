(ns saya.modules.ui.cursor
  (:require
   ["ink" :as k]
   ["strip-ansi" :default strip-ansi]
   [clojure.string :as str]))

(defonce ^:private shape-ref (atom :block))

; We use a couple zero-width characters that are highly unlikely
; to actually be used together:
; - Zero-width space
; - Zero-width non-joiner (should be between characters that normally
;   are rendered together with ligatures)
; - Another zero-width space
; (def ^:private cursor-text "\u200B\u200C\u200B")
(def ^:private cursor-text
  (str
   "\u001B]8;;" ; start link
   "saya://cursor" ; URL
   "\u001B\\" ; separator
   "\u200B" ; "Visible" text
   "\u001b]8;;\u001B\\"))
; (def ^:private cursor-text "\u001B[9999m\u200B")

(defn extract-cursor-position [lines]
  (loop [y 0
         lines lines]
    (when-some [line (first lines)]
      (if-let [raw-x (str/index-of line cursor-text)]
        (let [before (subs line 0 raw-x)
              x (count (strip-ansi before))]
          {:x x :y y})
        (recur (inc y)
               (next lines))))))

(defn get-cursor-shape []
  (or @shape-ref :block))

(defn strip-cursor
  "Trying to actually render our cursor text can cause lines
   to break in ways that the renderer can't diff correctly, so
   we just strip it out before rendering."
  [s]
  (when s
    (str/replace s cursor-text "")))

(defn- f>cursor [shape]
  ; HACKS: This should *really* be a useLayoutEffect, but
  ; that doesn't seem to consistently happen in time...?
  (reset! shape-ref shape)

  [:> k/Cursor {:shape (case shape
                         :block/blink "blockBlink"
                         :underscore/blink "underscoreBlink"
                         :pipe/blink "pipeBlink"
                         (name shape))}]
  #_[:> k/Text cursor-text])

(defn cursor
  ([] [cursor :block])
  ([shape] [:f> f>cursor shape]))
