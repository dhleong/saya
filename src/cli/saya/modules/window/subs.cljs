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
 (fn [[{:keys [height anchor-row]} lines]]
   (let [last-row-index (dec (count lines))
         anchor-row (or anchor-row
                        last-row-index)
         first-line-index (max 0 (- (inc anchor-row) height))]
     (->> lines
          ; Filter lines. We fill UP from the anchor-row
          (drop-last (- last-row-index anchor-row))
          (take-last height)

          ; Transform the line for rendering:
          (map (partial apply str))
          (map split/chars-with-ansi)

          ; Index properly, accounting for filtering
          (map-indexed (fn [i line]
                         [(+ i first-line-index) line]))))))

(reg-sub
 ::focused?
 :<- [:mode]
 :<- [:current-winnr]
 (fn [[mode current-winnr] [_ id]]
   (and (not= :command mode)
        (= current-winnr id))))
