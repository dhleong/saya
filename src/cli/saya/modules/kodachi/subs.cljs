(ns saya.modules.kodachi.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::state
 :-> :kodachi)
