(ns saya.modules.ui.cursor
  (:require
   ["ink" :as k]))

(defonce ^:private shape-ref (atom :block))

; NOTE: Kept around in case we need it in a transitional state
#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn get-cursor-shape []
  (or @shape-ref :block))

(defn- f>cursor [shape]
  ; HACKS: This should *really* be a useLayoutEffect, but
  ; that doesn't seem to consistently happen in time...?
  (reset! shape-ref shape)

  [:> k/Cursor (when shape
                 {:shape (case shape
                           :block/blink "blockBlink"
                           :underscore/blink "underscoreBlink"
                           :pipe/blink "pipeBlink"
                           (name shape))})])

(defn cursor
  ([] [cursor :block])
  ([shape] [:f> f>cursor shape]))
