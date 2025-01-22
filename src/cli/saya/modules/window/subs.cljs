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

(defn- format-lines-into-parts [lines]
  (loop [parts []
         chars (seq lines)]
    (if-some [ch (first chars)]
      (let [w (string-width ch)]
        (recur (if (> w 0)
                 (conj parts w)
                 (update parts (dec (count parts)) str ch))
               (next chars)))

      ; Done!
      parts)))

(reg-sub
 ::visible-lines
 (fn [[_ {:keys [winnr bufnr]}]]
   [(subscribe [::by-id winnr])
    (subscribe [::buffer-subs/ansi-lines-by-id bufnr])])
 (fn [[_window lines]]
   lines
   ; TODO: filter lines
   #_(format-lines-into-parts lines)))

(reg-sub
 ::focused?
 :<- [:mode]
 :<- [:current-winnr]
 (fn [[mode current-winnr] [_ id]]
   (and (not= :command mode)
        (= current-winnr id))))
