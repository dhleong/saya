(ns saya.modules.window.subs
  (:require
   [re-frame.core :refer [reg-sub subscribe]]
   [saya.modules.ansi.split :as split]
   [saya.modules.buffers.subs :as buffer-subs]))

(reg-sub
 ::by-id
 :<- [:windows]
 :=> get)

(reg-sub
 ::buffer-id
 (fn [[_ winnr]]
   (subscribe [::by-id winnr]))
 :-> :bufnr)

(reg-sub
 ::visible-lines
 (fn [[_ {:keys [winnr bufnr]}]]
   [(subscribe [::by-id winnr])
    (subscribe [::buffer-subs/ansi-lines-by-id bufnr])])
 (fn [[{:keys [height anchor-row] :or {height 10}} lines]]
   ; NOTE: height might be unavailable on the first render
   (let [last-row-index (dec (count lines))
         anchor-row (or anchor-row
                        last-row-index)
         first-line-index (max 0 (- (inc anchor-row) height))]
     (->> lines
          ; Filter lines. We fill UP from the anchor-row
          (drop-last (- last-row-index anchor-row))
          (take-last height)

          (into
           []
           (comp
             ; Transform the line for rendering:
            (map (fn [line]
                   (if (string? (first line))
                     (split/chars-with-ansi
                      (apply str line))

                     line
                     #_(do
                         (def last-thing line)
                         (first line)))))

             ; Index properly, accounting for filtering
            (map-indexed (fn [i line]
                           [(+ i first-line-index) line]))))))))

(reg-sub
 ::focused?
 :<- [:mode]
 :<- [:current-winnr]
 :<- [:current-buffer]
 (fn [[mode current-winnr current-buffer] [_ id]]
   (and (not= :command mode)
        (= current-winnr id)

        ; In insert mode for a connection buffer, we
        ; render a separate input window
        (not (and (= :insert mode)
                  (:connection-id current-buffer))))))

(reg-sub
 ::input-focused?
 :<- [:mode]
 :<- [:current-winnr]
 :<- [:current-buffer]
 (fn [[mode current-winnr current-buffer] [_ id]]
   (and (= :insert mode)
        (= current-winnr id)
        (:connection-id current-buffer))))

