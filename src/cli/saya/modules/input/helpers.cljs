(ns saya.modules.input.helpers
  (:require
   [saya.modules.buffers.line :refer [ansi-chars wrapped-lines]]
   [saya.modules.buffers.util :as buffers]
   [saya.modules.window.subs :refer [visible-lines]]))

(def ^:dynamic *mode* :normal)

(defn- nil-or-zero? [v]
  (or (nil? v)
      (= 0 v)))

(defn- cursor-anchor-offset [line width {cursor-col :col}]
  (loop [lines (wrapped-lines line width)
         offset (dec (count lines))]
    (if-some [{:keys [col line]} (first lines)]
      (if (< cursor-col (+ col (count line)))
        offset
        (recur (next lines)
               (dec offset)))

      ; After the last char on the line
      offset)))

(defn current-buffer-line [{:keys [lines cursor]}]
  (when (seq lines)
    (->> (nth lines (:row cursor))
         (ansi-chars))))

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

(defn derive-anchor-from-top-cursor
  "Given a cursor position at the 'top' of the screen, derive
   the anchor (row + offset) that would make that visible"
  [lines width start amount]
  (loop [anchor-row (:row start)
         anchor-offset (cursor-anchor-offset
                        (nth lines (:row start))
                        width
                        start)
         consumable (max 1 anchor-offset)
         to-consume (dec amount)]
    (cond
      (<= to-consume anchor-offset)
      {:anchor-row anchor-row
       :anchor-offset (- anchor-offset to-consume)}

      (< (inc anchor-row) (count lines))
      (let [next-row (inc anchor-row)
            wrapped-lines-count (count (wrapped-lines (nth lines next-row)
                                                      width))]
        (recur
         next-row
         (dec wrapped-lines-count)
         wrapped-lines-count
         (- to-consume consumable)))

      ; Bottom of the buffer, presumably
      :else
      {:anchor-row nil
       :anchor-offset nil})))

(defn clamp-scroll [{:keys [window buffer] :as ctx}]
  (let [{:keys [height width anchor-offset anchor-row]} window]
    (cond
      (and (>= anchor-row (last-buffer-row buffer))
           (nil-or-zero? anchor-offset))
      (update ctx :window dissoc :anchor-row)

      (and anchor-row
           (< (- anchor-row height) 0))
      (update ctx :window merge (derive-anchor-from-top-cursor
                                 (:lines buffer)
                                 width
                                 {:row 0 :col 0}
                                 height))

      ; Nothing to fix:
      :else ctx)))

(defn adjust-scroll-to-cursor [{:keys [buffer window] :as ctx}]
  (let [{:keys [height width anchor-offset anchor-row]} window
        {:keys [row] :as cursor} (:cursor buffer)
        anchor-row (or anchor-row
                       (last-buffer-row buffer))]
    (cond
      (and (>= row (last-buffer-row buffer))
           (nil-or-zero? anchor-offset))
      (update ctx :window dissoc :anchor-row)

      ; Derive anchor-row/offset
      ; TODO: Improve within-row offset handling
      (< row anchor-row)
      (let [from-cursor (derive-anchor-from-top-cursor
                         (:lines buffer)
                         width
                         cursor
                         height)]
        ; NOTE: If deriving from the cursor would put the scroll "lower"
        ; (IE: closer to the bottom of the screen) than it is, then our
        ; cursor is visible! Only if it would put the scroll "higher" do
        ; we need to apply
        (if (or (< (:anchor-row from-cursor)
                   anchor-row)
                (and (= (:anchor-row from-cursor)
                        anchor-row)
                     (> (:anchor-offset from-cursor)
                        anchor-offset)))
          (update ctx :window merge from-cursor)
          ctx))

      (> row anchor-row)
      (assoc-in ctx [:window :anchor-row] row)

      ; Nothing to fix:
      :else ctx)))

(defn- visible-cursor-range
  ([ctx] (visible-cursor-range ctx nil))
  ([{:keys [window buffer]} {:keys [preserve-col?]}]
   ; NOTE: We *could* use a Flow to cache this in the DB and
   ; avoid having to recompute on the fly?
   (let [all-visible (visible-lines window (:lines buffer))]
     [(let [{:keys [row col]} (first all-visible)]
        {:row row :col col})

      (let [{:keys [row col line last-of-line?]} (peek all-visible)]
        {:row row
         :col (if preserve-col?
                (let [cursor-line (nth (:lines buffer) (:row (:cursor buffer)))
                      offset (cursor-anchor-offset
                              cursor-line
                              (:width window)
                              (:cursor buffer))
                      cursor-wrapped-line (->>
                                           (wrapped-lines
                                            cursor-line
                                            (:width window))
                                           (drop-last offset)
                                           (last))
                      cursor-relative-col (- (:col (:cursor buffer))
                                             (:col cursor-wrapped-line))]
                  (+ col cursor-relative-col))

                (cond-> (+ col (count line))
                  (not last-of-line?) (dec)))})])))

(defn adjust-cursor-to-scroll [ctx]
  (let [visible-range (visible-cursor-range ctx {:preserve-col? true})]
    (update-in ctx [:buffer :cursor] buffers/clamp-cursor visible-range)))

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
   ; NOTE: clamp-scroll is redundant now with adjust-scroll-to-cursor;
   ; we should only need it when directly adjusting scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn cursor-updator [ctx]
     (update-in ctx [:buffer :cursor col-or-row] f))))

