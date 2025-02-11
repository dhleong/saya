(ns saya.util.ink
  (:require
   ["ansi-escapes" :as ansi]
   [applied-science.js-interop :as j]
   [archetype.util :refer [>evt]]
   [clojure.string :as str]
   [saya.modules.ui.cursor :refer [extract-cursor-position get-cursor-shape
                                   strip-cursor]]))

(defn- ansi-cursor [v]
  (str "\u001B[" v " q"))

(defn update-screen [{:keys [out last-lines]
                      :as state}
                     output]
  (let [lines (str/split-lines output)
        metrics (atom {})
        cursor-shape (get-cursor-shape)]
    (if-not (= (count lines)
               (count last-lines))
      ; Either this is the first render, of the lines count
      ; has changed (perhaps due to a resize). Just start
      ; from scratch:
      (let [to-render (strip-cursor output)]
        (reset! metrics {:full-render? {:before (count last-lines)
                                        :after (count lines)}})
        (.write out ansi/clearTerminal)
        (.write out to-render))

      ; Diff each line
      (doseq [i (range (count lines))]
        ; NOTE: We diff *without* the cursor, since we don't need
        ; to re-render the whole line if just the cursor position changed!
        (let [last (strip-cursor (nth last-lines i))
              this (strip-cursor (nth lines i))]
          (when-not (= last this)
            (swap! metrics update :dirty-lines (fnil inc 0))
            (.write out (ansi/cursorTo 0 i))
            (.write out ansi/eraseLine)
            (.write out this)))))

    (if-let [{:keys [x y] :as position} (extract-cursor-position lines)]
      (do
        (>evt [:saya.events/set-global-cursor position])
        (swap! metrics assoc :moved-cursor [x y cursor-shape])
        (.write out (ansi/cursorTo x y))
        (.write out (case cursor-shape
                      :block/blink (ansi-cursor 1)
                      :block (ansi-cursor 2)
                      :underscore/blink (ansi-cursor 3)
                      :underscore (ansi-cursor 4)
                      :pipe/blink (ansi-cursor 5)
                      :pipe (ansi-cursor 6)))
        (.write out ansi/cursorShow))

      (do
        (>evt [:saya.events/set-global-cursor nil])
        (.write out ansi/cursorHide)))

    (-> state
        (update :history (fnil conj []) lines)
        (update :metrics-history (fnil conj []) @metrics)
        (assoc
         :last-cursor cursor-shape
         :last-metrics @metrics
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
  (take-last 5 (map count (:history @@last-state)))

  (last (butlast (:history @@last-state)))
  (last (:history @@last-state))
  (extract-cursor-position (:last-lines @@last-state))

  (count (:history @@last-state))
  (:metrics-history @@last-state)
  (:last-cursor @@last-state)
  (:last-metrics @@last-state))
