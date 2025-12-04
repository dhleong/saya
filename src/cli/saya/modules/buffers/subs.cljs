(ns saya.modules.buffers.subs
  (:require
   [re-frame.core :refer [reg-sub]]
   [re-frame.subs :refer [subscribe]]))

(reg-sub
 ::by-id
 :<- [:buffers]
 :=> get)

(reg-sub
 ::lines-by-id
 (fn [[_ id]]
   (subscribe [::by-id id]))
 :-> :lines)

(reg-sub
 ::->connr
 (fn [[_ id]]
   (subscribe [::by-id id]))
 :-> :connection-id)

(reg-sub
 ::current-buffer-cursor
 :<- [:buffers]
 :<- [:current-bufnr]
 :<- [:mode]
 (fn [[buffers current-bufnr mode] [_ buffer-id]]
   (when (and (= current-bufnr buffer-id)
              (not= :prompt mode))
     (:cursor (get buffers buffer-id)))))

(reg-sub
 ::buffer-cursor
 :<- [:buffers]
 (fn [buffers [_ buffer-id]]
   (:cursor (get buffers buffer-id))))
