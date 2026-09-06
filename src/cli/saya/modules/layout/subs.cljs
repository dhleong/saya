(ns saya.modules.layout.subs
  (:require
   [re-frame.core :refer [reg-sub subscribe]]
   [saya.modules.layout.core :as layout]))

(reg-sub
 ::by-id
 :-> :layouts)

(reg-sub
 ::with-id
 :<- [::by-id]
 :=> get)

(reg-sub
 ::current
 :<- [::with-id 0]
 :-> identity)

(reg-sub
 ::evaluated
 (fn [[_ layout-id]]
   (subscribe [::with-id layout-id]))
 :-> layout/evaluate)

(reg-sub
 ::all-keys
 :-> :layout/keys)

(reg-sub
 ::key
 :<- [::all-keys]
 :=> get)
