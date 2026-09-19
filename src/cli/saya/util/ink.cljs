(ns saya.util.ink
  (:require
   [applied-science.js-interop :as j]))

; Grab a reference at declare time to avoid conflict with
; log patching
(def original-stdout js/process.stdout)

(defn stdout
  ([] (stdout {} original-stdout))
  ([opts ^js out]
   (stdout opts (atom {:out out}) out))
  ([_opts state ^js out]
   ; (reset! last-state state)

   (js/Object.defineProperties
    (j/obj .-write (partial swap! state
                            (fn [_ str]
                              (.write out str)
                              {:last-output str}))
           .-on (.bind (.-on out) out)
           .-off (.bind (.-off out) out)
           :original-stream out
           :saya? true)
    #js {:rows #js {:get #(.-rows out)}
         :columns #js {:get #(.-columns out)}
         :isTTY #js {:get #(.-isTTY out)}})))
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

