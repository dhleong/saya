(ns saya.modules.search.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::input-text
 :-> (constantly ""))
