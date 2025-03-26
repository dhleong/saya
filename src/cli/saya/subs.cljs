(ns saya.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub :page :-> :page)
(reg-sub :dimens :-> :dimens)
(reg-sub :mode :-> :mode)

(reg-sub :windows :-> :windows)
(reg-sub :buffers :-> :buffers)
(reg-sub :current-winnr :-> :current-winnr)
(reg-sub :last-winnr :-> :last-winnr)

(reg-sub
 :current-window
 :<- [:windows]
 :<- [:current-winnr]
 (fn [[windows winnr]]
   (get windows winnr)))

(reg-sub
 :current-bufnr
 :<- [:current-window]
 (fn [window]
   (:bufnr window)))

(reg-sub
 :current-buffer
 :<- [:buffers]
 :<- [:current-bufnr]
 (fn [[buffers bufnr]]
   (get buffers bufnr)))

(reg-sub
 :global-cursor
 :-> :cursor)

; ======= Echo =============================================

(reg-sub
 ::echo-history
 :-> :echo-history)

(reg-sub
 ::echo-ack-pending-since
 :-> :echo-ack-pending-since)

(reg-sub
 ::echo-cleared-at
 :-> :echo-cleared-at)

(reg-sub
 :echo-lines
 :<- [::echo-history]
 :<- [::echo-cleared-at]
 :<- [::echo-ack-pending-since]
 (fn [[history echo-cleared-at ack-pending-since]]
   (or (when ack-pending-since
         (->> history
              (reverse)
              (take-while (fn [{:keys [timestamp]}]
                            (>= timestamp ack-pending-since)))
              (reverse)))

       ; Last echo if not cleared
       (let [latest (peek history)]
         (when (or (not echo-cleared-at)
                   (< echo-cleared-at
                      (:timestamp latest)))
           [latest])))))
