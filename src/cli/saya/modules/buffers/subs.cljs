(ns saya.modules.buffers.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::id->obj
 :-> :buffers)

(reg-sub
 ::by-id
 :<- [::id->obj]
 (fn [buffers [_ id]]
   (get buffers id)))
