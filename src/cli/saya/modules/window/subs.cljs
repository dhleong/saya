(ns saya.modules.window.subs
  (:require
   ["string-width" :default string-width]
   [re-frame.core :refer [reg-sub subscribe]]
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

(defn- format-line-into-parts [line]
  (loop [parts []
         chars (seq line)]
    (if-some [ch (first chars)]
      (let [w (string-width ch)]
        (recur (cond
                 (> w 0)
                 (conj parts ch)

                 (seq parts)
                 (update parts (dec (count parts)) str ch)

                 :else
                 (conj parts ch))
               (next chars)))

      ; Done!
      parts)))

(reg-sub
 ::visible-lines
 (fn [[_ {:keys [winnr bufnr]}]]
   [(subscribe [::by-id winnr])
    (subscribe [::buffer-subs/ansi-lines-by-id bufnr])])
 (fn [[_window lines]]
   ; TODO: filter lines
   (->> lines
        (map format-line-into-parts)
        ; TODO: line index properly, accounting for filtering
        (map-indexed vector))))

(reg-sub
 ::focused?
 :<- [:mode]
 :<- [:current-winnr]
 (fn [[mode current-winnr] [_ id]]
   (and (not= :command mode)
        (= current-winnr id))))
