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
                     buffer-lines]
  ; NOTE: height might be unavailable on the first render
  (let [last-row-index (dec (count buffer-lines))
        anchor-row (or anchor-row
                       last-row-index)
        first-line-index (max 0 (- anchor-row (dec height)))]
    (->>
      ; Filter lines. We fill UP from (including!) the anchor-row
     (subvec buffer-lines first-line-index (inc anchor-row))

     (into
      []
      (comp
       ; Transform the line for rendering within the window:
       (map #(wrapped-lines % width))

       ; Index properly, accounting for filtering
       (map-indexed (fn [relative-row wrapped-lines]
                      (let [last-wrapped-index (dec (count wrapped-lines))]
                        (map-indexed
                         (fn [wrapped-index line]
                           (assoc line
                                  :row (+ relative-row first-line-index)
                                  :last-of-row? (= wrapped-index
                                                   last-wrapped-index)))
                         wrapped-lines))))
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
 (fn [[window buffer buffer-lines]]
   (or (seq (when buffer-lines
              (visible-lines window buffer-lines)))

       ; NOTE: Non-connection buffers need some blank "starter" line
       ; for editing purposes
       (when-not (:connection-id buffer)
         [{:row 0 :col 0 :line [] :last-of-row? true}]))))

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
        (str/join "\n"))))
(reg-sub
 ::connections
 :-> :connections)

(reg-sub
 ::connection-by-id
 :<- [::connections]
 :=> get)

(reg-sub
 ::single-prompt
 (fn [[_ connr]]
   (subscribe [::connection-by-id connr]))
 (fn [{:keys [prompts]} _]
   (when (and (= 1 (count prompts))
              (= 1 (count (get prompts 0))))
     (get-in prompts [0 0]))))
