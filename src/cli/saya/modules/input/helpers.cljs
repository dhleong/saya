(ns saya.modules.input.helpers
  (:require
   [saya.modules.ansi.split :as split]))

(defn- current-buffer-line [{:keys [lines cursor]}]
  ; TODO: We should probably just store the :plain line...
  (->> (nth lines (:row cursor))
       (map :ansi)
       (apply str)
       (split/chars-with-ansi)))

(defn- last-buffer-row [buffer]
  (max 0
       (dec (count (:lines buffer)))))

(defn adjust-scroll [{:keys [window buffer] :as ctx}]
  (let [{:keys [height anchor-row]} window
        {:keys [row]} (:cursor buffer)]
    (cond-> ctx
      (= row (last-buffer-row buffer))
      (update :window dissoc :anchor-row)

      (< (- anchor-row height) 0)
      (assoc-in [:window :anchor-row] (dec height)))))

(defn clamp-cursor [{:keys [window buffer] :as ctx}]
  (let [{:keys [height anchor-row]} window
        anchor-row (or anchor-row
                       (last-buffer-row buffer))
        min-cursor-row (max 0 (- anchor-row height))
        max-cursor-row (min (last-buffer-row buffer)
                            (+ min-cursor-row height))]
    (-> ctx
        (update-in [:buffer :cursor :row]
                   #(min max-cursor-row
                         (max min-cursor-row %)))

        ; NOTE: We delay reading the current buffer line until
        ; after clamping the row above, since it may have changed!
        (as-> ctx
          (let [max-cursor-col (count (current-buffer-line (:buffer ctx)))]
            (update-in ctx [:buffer :cursor :col]
                       #(min max-cursor-col
                             (max 0 %))))))))
