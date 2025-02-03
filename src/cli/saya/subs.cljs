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
