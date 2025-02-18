(ns saya.modules.window.subs
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-sub subscribe]]
   [saya.modules.buffers.line :refer [wrapped-lines]]
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

(defn visible-lines [{:keys [height width anchor-offset anchor-row]
                      :or {height 10 width 50}}
                     ansi-lines]
  ; NOTE: height might be unavailable on the first render
  (let [last-row-index (dec (count ansi-lines))
        anchor-row (or anchor-row
                       last-row-index)
        first-line-index (max 0 (- (inc anchor-row) height))]
    (->>
      ; Filter lines. We fill UP from (including!) the anchor-row
     (subvec ansi-lines (max 0 (- anchor-row (dec height))) (inc anchor-row))

     (into
      []
      (comp
          ; Transform the line for rendering within the window:
       (map #(wrapped-lines % width))

          ; Index properly, accounting for filtering
       (map-indexed (fn [i lines]
                      (map
                       (fn [line]
                         (assoc line :row (+ i first-line-index)))
                       lines)))
       (mapcat identity)))

     ; Apply anchor offset + height limit:

     ((fn [wrapped]
        (let [end (max 0 (- (count wrapped)
                            anchor-offset))]
          (subvec wrapped (max 0 (- end height)) end)))))))

(reg-sub
 ::visible-lines
 (fn [[_ {:keys [winnr bufnr]}]]
   [(subscribe [::by-id winnr])
    (subscribe [::buffer-subs/by-id bufnr])
    (subscribe [::buffer-subs/lines-by-id bufnr])])
 (fn [[window buffer ansi-lines]]
   (or (seq (visible-lines window ansi-lines))

       ; NOTE: Non-connection buffers need some blank "starter" line
       ; for editing purposes
       (when-not (:connection-id buffer)
         [{:row 0 :col 0 :line []}]))))

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

(reg-sub
 ::scrolled?
 (fn [[_ winnr]]
   (subscribe [::by-id winnr]))
 (fn [window]
   (some? (:anchor-row window))))

(reg-sub
 ::input-text
 (fn [[_ connr]]
   (subscribe [::buffer-subs/by-id [:conn/input connr]]))
 (fn [buffer]
   (->> (:lines buffer)
        (map (partial mapcat :ansi))
        (map (comp (partial apply str)))
        (str/join "\n"))))
