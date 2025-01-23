(ns saya.modules.buffers.subs
  (:require
   [re-frame.core :refer [reg-sub]]
   [re-frame.subs :refer [subscribe]]))

(reg-sub
 ::by-id
 :<- [:buffers]
 :=> get)

(reg-sub
 ::ansi-lines-by-id
 (fn [[_ id]]
   (subscribe [::by-id id]))
 :-> (fn [buffer]
       (some->> buffer
                :lines
                (map (fn [line]
                       (map :ansi line))))))

(reg-sub
 ::->connr
 (fn [[_ id]]
   (subscribe [::by-id id]))
 :-> :connection-id)

(reg-sub
 ::buffer-cursor
 :<- [:buffers]
 :<- [:current-bufnr]
 (fn [[buffers current-bufnr] [_ buffer-id]]
   (when (= current-bufnr buffer-id)
     (:cursor (get buffers buffer-id)))))
