(ns saya.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub :page :-> :page)
(reg-sub :dimens :-> :dimens)
(reg-sub :mode :-> :mode)

(reg-sub :windows :-> :windows)
(reg-sub :buffers :-> :buffers)
(reg-sub :current-winnr :-> :current-winnr)

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

