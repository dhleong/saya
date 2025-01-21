(ns saya.util.ink
  (:require
   ["ansi-escapes" :as ansi]
   [applied-science.js-interop :as j]
   [clojure.string :as str]
   [saya.modules.ui.cursor :refer [extract-cursor-position get-cursor-shape]]))

(defn- ansi-cursor [v]
  (str "\u001B[" v " q"))

(defn update-screen [{:keys [out last-lines]
                      :as state}
                     output]
  (let [lines (str/split-lines output)]
    (if-not (= (count lines)
               (count last-lines))
      ; Either this is the first render, of the lines count
      ; has changed (perhaps due to a resize). Just start
      ; from scratch:
      (do
        (.write out ansi/clearTerminal)
        (.write out output))

      ; Diff each line
      (doseq [i (range (count lines))]
        (let [last (nth last-lines i)
              this (nth lines i)]
          (when-not (= last this)
            (.write out (ansi/cursorTo 0 i))
            (.write out ansi/eraseLine)
            (.write out this)))))

    (if-let [{:keys [x y]} (extract-cursor-position lines)]
      (let [shape (get-cursor-shape)]
        (.write out (ansi/cursorTo x y))
        (.write out (case shape
                      :block/blink (ansi-cursor 1)
                      :block (ansi-cursor 2)
                      :underscore/blink (ansi-cursor 3)
                      :underscore (ansi-cursor 4)
                      :pipe/blink (ansi-cursor 5)
                      :pipe (ansi-cursor 6)))
        (.write out ansi/cursorShow))
      (.write out ansi/cursorHide))

    (-> state
        (update :history (fnil conj []) lines)
        (assoc
         :last-lines lines
         :last-output output))))

(defn- strip-provided-ansi [output]
  ; Let update-screen work with a clean slate. Since we're
  ; always a "full screen" app, output *should always* start
  ; with clearTerminal. Occasionally, however, ink tries
  ; to render an incomplete frame or something. We ignore those,
  ; since they typically do things like "clear N lines" which
  ; will break things unexpectedly, and *should* be followed by
  ; a normal "full screen" render.
  (when (str/starts-with? output ansi/clearTerminal)
    (subs output (count ansi/clearTerminal))))

(defonce ^:private last-state (atom nil))

(defn stdout []
  (let [out js/process.stdout
        state (atom {:out out})]

    (reset! last-state state)

    (js/Object.defineProperties
     (j/obj .-write (partial swap! state
                             (fn [state str]
                               (if-let [str (strip-provided-ansi str)]
                                 (update-screen state str)
                                 state)))
            .-on (.bind (.-on out) out)
            .-off (.bind (.-off out) out))
     #js {:rows #js {:get #(.-rows out)}
          :columns #js {:get #(.-columns out)}})))

(comment
  (map count (:history @@last-state)))
