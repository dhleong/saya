(ns saya.modules.buffers.subs
  (:require
   [re-frame.core :refer [reg-sub]]
   [re-frame.subs :refer [subscribe]]))

(reg-sub
 ::id->obj
 :-> :buffers)

(reg-sub
 ::by-id
 :<- [::id->obj]
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
