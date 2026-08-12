(ns saya.util.ink
  (:require
   ["ansi-escapes" :as ansi]
   ["ink" :as k]
   ["node:fs" :as fs]
   [applied-science.js-interop :as j]
   [archetype.util :refer [>evt]]
   [clojure.string :as str]
   [promesa.core :as p]
   [saya.modules.ui.cursor :refer [extract-cursor-position get-cursor-shape
                                   strip-cursor]]))

; Grab a reference at declare time to avoid conflict with
; log patching
(def ^:private original-stdout js/process.stdout)

; Can be helpful for debugging:
(def ^:private log-file (when-some [path js/process.env.INK_DEBUG_LOG]
                          (fs/createWriteStream
                           path
                           #js {:flush true})))

(defn- log-write [out s & context]
  (.write out s)

  (when (and log-file (seq context))
    (.write log-file "->")
    (.write log-file (-> (str/join
                          " <-- "
                          (cons s context))
                         (str/replace "\u001B" "ESC")))
    (.write log-file "\n")))

(defn- ansi-cursor [v]
  (str "\u001B[" v " q"))

(defn- ansi-cursor-shape [cursor-shape]
  (case cursor-shape
    :block/blink (ansi-cursor 1)
    :block (ansi-cursor 2)
    :underscore/blink (ansi-cursor 3)
    :underscore (ansi-cursor 4)
    :pipe/blink (ansi-cursor 5)
    :pipe (ansi-cursor 6)))

(defn- stdout->dimens [out]
  {:w (j/get out .-columns)
   :h (j/get out .-rows)})

(defn update-screen [{:keys [out last-lines cursor-shape? position-cursor?]
                      :or {cursor-shape? true
                           position-cursor? true}
                      :as state}
                     output]
  (let [lines (str/split-lines output)
        metrics (atom {})
        cursor-shape (get-cursor-shape)
        dimens (stdout->dimens out)
        literal-ansi? (#{ansi/exitAlternativeScreen
                         ansi/enterAlternativeScreen
                         ansi/showCursorEscape}
                       output)]
    (cond
      literal-ansi?
      (log-write out output "literal ansi")

      (not= dimens
            (:last-dimens state))
      ; Either this is the first render, of the lines count
      ; has changed (perhaps due to a resize). Just start
      ; from scratch:
      (let [to-render (strip-cursor output)]
        (reset! metrics {:full-render? {:before (count last-lines)
                                        :after (count lines)}})
        (log-write out ansi/clearViewport "scratch")
        (log-write out to-render))

      ; Diff each line
      :else
      (doseq [i (range (count lines))]
        ; NOTE: We diff *without* the cursor, since we don't need
        ; to re-render the whole line if just the cursor position changed!
        (let [last (strip-cursor (nth last-lines i nil))
              this (strip-cursor (nth lines i))]
          (when-not (= last this)
            (swap! metrics update :dirty-lines (fnil inc 0))
            (log-write out (ansi/cursorTo 0 i) "diff @ " i)
            (log-write out ansi/eraseLine)
            (log-write out this)))))

    ; Erase down any lines we'd previously rendered but no longer do
    (when-not literal-ansi?
      (when (> (count last-lines)
               (count lines))
        (swap! metrics update :dirty-lines + (- (count last-lines)
                                                (count lines)))
        (log-write out (ansi/cursorTo 0 (count lines)) "extra lines")
        (log-write out ansi/eraseDown))

      (if-let [{:keys [x y] :as position} (extract-cursor-position lines)]
        (do
          (when position-cursor?
            (>evt [:saya.events/set-global-cursor position]))
          (swap! metrics assoc :moved-cursor [x y cursor-shape])
          (log-write out (ansi/cursorTo x y) "move to " x y)
          (when cursor-shape?
            (log-write out (ansi-cursor-shape cursor-shape)))
          (log-write out ansi/cursorShow "position-cursor? show"))

        (do
          (when position-cursor?
            (>evt [:saya.events/set-global-cursor nil]))
          (log-write out ansi/cursorHide "position-cursor? hide"))))

    (-> state
        (update :history (fnil conj []) lines)
        (update :metrics-history (fnil conj []) @metrics)
        (assoc
          ; This is a somewhat janky way to skip any extra render
          ; requests from Ink after we have exited the alternative
          ; screen. This hack ensures we cleanly restore the
          ; original screen state on quit
         :exited? (= ansi/exitAlternativeScreen output)
         :last-dimens dimens
         :last-cursor cursor-shape
         :last-metrics @metrics
         :last-lines lines
         :last-output output))))

(defonce ^:private last-state (atom nil))

(defn stdout
  ([] (stdout {} original-stdout))
  ([opts ^js out]
   (stdout opts (atom {:out out}) out))
  ([opts state ^js out]
   (reset! last-state state)

   (js/Object.defineProperties
    (j/obj .-write (partial swap! state
                            (fn [{:keys [exited?] :as state} str]
                              (when-not exited?
                                (cond
                                  (some? str)
                                  (update-screen state str)

                                  (and (:always-render? opts)
                                       (not= ansi/clearTerminal str))
                                  (update-screen state str)

                                  :else
                                  state))))
           .-on (.bind (.-on out) out)
           .-off (.bind (.-off out) out)
           :original-stream out
           :saya? true)
    #js {:rows #js {:get #(.-rows out)}
         :columns #js {:get #(.-columns out)}
         :isTTY #js {:get #(.-isTTY out)}})))

(defn- unmount [^js saya-stdout]
  (let [raw-stdout (j/get saya-stdout :original-stream)]
    (when-not (= :block (get-cursor-shape))
      ; Reset cursor
      (log-write raw-stdout (ansi-cursor-shape :block) "unmount"))
    (log-write raw-stdout ansi/cursorShow "unmount -> show cursor")))

(defn ->exit-promise [^js instance]
  (.waitUntilExit instance))

(defn render-alternate [app opts]
  (let [stdout (if (j/get-in opts [:stdout :saya?])
                 (j/get opts :stdout)
                 (stdout {} (j/get opts :stdout original-stdout)))
        opts (j/assoc! opts
                       ; NOTE: Without :debug true, ink tries
                       ; to do throttling and its own diffing
                       ; and cursor movement, etc.
                       :debug true
                       :alternateScreen true
                       :stdout stdout)
        ink (k/render app opts)]
    (-> (->exit-promise ink)
        (p/then #(unmount stdout)))
    ink))

(comment
  (take-last 5 (map count (:history @@last-state)))

  (.on js/process "beforeExit" #(println @@last-state))

  (last (butlast (:history @@last-state)))
  (last (:history @@last-state))
  (extract-cursor-position (:last-lines @@last-state))

  (count (:history @@last-state))
  (:metrics-history @@last-state)
  (:last-cursor @@last-state)
  (:last-metrics @@last-state))
