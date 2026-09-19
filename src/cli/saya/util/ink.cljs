(ns saya.util.ink)

; Grab a reference at declare time to avoid conflict with
; log patching
(def original-stdout js/process.stdout)

(defn- ansi-cursor [v]
  (str "\u001B[" v " q"))

; NOTE: Kept around in case we need it in a transitional state
#_{:clj-kondo/ignore [:unused-private-var]}
(defn- ansi-cursor-shape [cursor-shape]
  (case cursor-shape
    :block/blink (ansi-cursor 1)
    :block (ansi-cursor 2)
    :underscore/blink (ansi-cursor 3)
    :underscore (ansi-cursor 4)
    :pipe/blink (ansi-cursor 5)
    :pipe (ansi-cursor 6)))

(defn ->exit-promise [^js instance]
  (.waitUntilExit instance))

