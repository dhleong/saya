(ns saya.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub :page :page)
(reg-sub :dimens :dimens)
(reg-sub :mode :mode)
