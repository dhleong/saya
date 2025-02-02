(ns saya.cli.keys
  (:require
   [applied-science.js-interop :as j]))

(def ^:private keyword-dispatchables
  [:upArrow :downArrow :leftArrow :rightArrow
   :pageDown :pageUp :return :escape :tab :backspace :delete])

(def ^:private keyword-renames
  {:upArrow :up
   :downArrow :down
   :leftArrow :left
   :rightArrow :right
   :pageUp :page-up
   :pageDown :page-down})

; Keys that always seem to have "shift" set, so we ignore
(def ^:private always-shift?
  #{:return})

; Keys that always seem to have "meta" set, so we ignore
(def ^:private always-meta?
  #{:escape})

; HACKS?
(def ^:private input-mapped
  {"[13;2u" :shift/return
   "[13;5u" :ctrl/return
   "[9;5u" :ctrl/tab})

(defn ->key [input k]
  #_{:clj-kondo/ignore [:inline-def :unused-private-var]}
  (def ^:private last-params [input k])

  (let [kw (some #(when (j/get k %) %) keyword-dispatchables)
        kw (get keyword-renames kw kw)]
    (or (when (and kw
                   (j/get k :shift)
                   (not (always-shift? kw)))
          (keyword "shift" (name kw)))

        (when (and kw
                   (j/get k :meta)
                   (not (always-meta? kw)))
          (keyword "meta" (name kw)))

        ; NOTE: :tab, for example, seems to always have :ctrl as "true"
        ;  so we only bother with "shift" for now...
        kw

        ; For normal keys, since letters, for example, will have both shift and
        ; the capitalized letter, we only bother with ctrl/meta for now...
        (when (j/get k :ctrl)
          (keyword "ctrl" input))

        (when (j/get k :meta)
          ; AKA: "alt" key
          (keyword "meta" input))

        (get input-mapped input)

        input)))
