(ns saya.modules.input.helpers
  (:require
   [saya.modules.ansi.split :as split]))

(def ^:dynamic *mode* :normal)

(defn current-buffer-line [{:keys [lines cursor]}]
  ; TODO: We should probably just store the :plain line...
  (->> (nth lines (:row cursor))
       (map :ansi)
       (apply str)
       (split/chars-with-ansi)))

(defn last-buffer-row [buffer]
  (max 0
       (dec (count (:lines buffer)))))

(defn current-buffer-line-last-col [buffer]
  (let [after-last-col (count (current-buffer-line buffer))]
    (max 0
         (case *mode*
           :insert after-last-col

           ; Usually, we don't want to be *after* the last col, we want to be *on* it
           (dec after-last-col)))))

(defn clamp-scroll [{:keys [window buffer] :as ctx}]
  (let [{:keys [height anchor-row]} window]
    (cond
      (>= anchor-row (last-buffer-row buffer))
      (update ctx :window dissoc :anchor-row)

      (and anchor-row
           (< (- anchor-row height) 0))
      (assoc-in ctx [:window :anchor-row] (dec height))

      ; Nothing to fix:
      :else ctx)))

(defn adjust-scroll-to-cursor [{:keys [buffer window] :as ctx}]
  (let [{:keys [height anchor-row]} window
        {:keys [row]} (:cursor buffer)
        anchor-row (or anchor-row
                       (last-buffer-row buffer))]
    (cond
      (>= row (last-buffer-row buffer))
      (update ctx :window dissoc :anchor-row)

      (<= row (- anchor-row height))
      (assoc-in ctx [:window :anchor-row] (+ row (dec height)))

      (> row anchor-row)
      (assoc-in ctx [:window :anchor-row] row)

      ; Nothing to fix:
      :else ctx)))

(defn adjust-cursor-to-scroll [{:keys [window buffer] :as ctx}]
  (let [{:keys [height anchor-row]} window
        anchor-row (or anchor-row
                       (last-buffer-row buffer))
        min-cursor-row (max 0 (inc (- anchor-row height)))
        max-cursor-row (min (last-buffer-row buffer)
                            (+ min-cursor-row height))]
    (update-in ctx [:buffer :cursor :row]
               #(min max-cursor-row
                     (max min-cursor-row %)))))

(defn clamp-cursor [{:keys [window buffer] :as ctx}]
  (let [{:keys [height anchor-row]} window
        anchor-row (or anchor-row
                       (last-buffer-row buffer))
        min-cursor-row (max 0 (- anchor-row height))
        max-cursor-row (last-buffer-row buffer)]
    (-> ctx
        (update-in [:buffer :cursor :row]
                   #(min max-cursor-row
                         (max min-cursor-row %)))

        ; NOTE: We delay reading the current buffer line until
        ; after clamping the row above, since it may have changed!
        (as-> ctx
          (let [max-cursor-col (current-buffer-line-last-col (:buffer ctx))]
            (update-in ctx [:buffer :cursor :col]
                       #(min max-cursor-col
                             (max 0 %))))))))

(defn update-cursor [col-or-row f]
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn cursor-updator [ctx]
     (update-in ctx [:buffer :cursor col-or-row] f))))

