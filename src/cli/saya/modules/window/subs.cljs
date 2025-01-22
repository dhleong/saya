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
 (fn [[_window lines]]
   ; TODO: filter lines
   (->> lines
        (map (partial apply str))
        (map split/chars-with-ansi)
        ; TODO: line index properly, accounting for filtering
        (map-indexed vector))))

(reg-sub
 ::focused?
 :<- [:mode]
 :<- [:current-winnr]
 (fn [[mode current-winnr] [_ id]]
   (and (not= :command mode)
        (= current-winnr id))))
