(ns saya.modules.input.helpers
  (:require
   [saya.cli.text-input.helpers :refer [dec-to-zero]]
   [saya.modules.buffers.line :refer [ansi-chars wrapped-lines]]))

(def ^:dynamic *mode* :normal)

(defn- nil-or-zero? [v]
  (or (nil? v)
      (= 0 v)))

(defn current-buffer-line [{:keys [lines cursor]}]
  (->> (nth lines (:row cursor))
       (ansi-chars)))

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
  (let [{:keys [height anchor-offset anchor-row]} window]
    (cond
      (and (>= anchor-row (last-buffer-row buffer))
           (nil-or-zero? anchor-offset))
      (update ctx :window dissoc :anchor-row)

      (and anchor-row
           (< (- anchor-row height) 0))
      (assoc-in ctx [:window :anchor-row] (dec height))

      ; Nothing to fix:
      :else ctx)))

(defn adjust-scroll-to-cursor [{:keys [buffer window] :as ctx}]
  (let [{:keys [height anchor-offset anchor-row]} window
        {:keys [row]} (:cursor buffer)
        anchor-row (or anchor-row
                       (last-buffer-row buffer))]
    (cond
      (and (>= row (last-buffer-row buffer))
           (nil-or-zero? anchor-offset))
      (update ctx :window dissoc :anchor-row)

      (<= row (- anchor-row height))
      (assoc-in ctx [:window :anchor-row] (+ row (dec height)))

      (> row anchor-row)
      (assoc-in ctx [:window :anchor-row] row)

      ; Nothing to fix:
      :else ctx)))

(defn adjust-cursor-to-scroll [{:keys [window buffer] :as ctx}]
  (let [{:keys [height width anchor-offset anchor-row]} window
        anchor-row (or anchor-row
                       (last-buffer-row buffer))
        min-cursor-row (max 0 (inc (- anchor-row height)))
        max-cursor-row (min (last-buffer-row buffer)
                            (+ min-cursor-row (dec height)))
        {:keys [col row]} (:cursor buffer)
        row' (min max-cursor-row
                  (max min-cursor-row row))
        wrapped (wrapped-lines
                 (get-in buffer [:lines row])
                 width)]
    (cond-> (assoc-in ctx [:buffer :cursor :row] row')
      ; Adjust :col to be within the visible part of the line
      (= anchor-row row)
      (as-> ctx
        (let [visible-lines (drop-last anchor-offset wrapped)
              last-visible-col (dec-to-zero
                                (+ (:col (last visible-lines))
                                   (count (last visible-lines))))]
          (cond-> ctx
            (> col last-visible-col)
            (assoc-in [:buffer :cursor :col] last-visible-col)))))))

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

