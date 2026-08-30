(ns saya.modules.input.history.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::history-search
 :-> :history-search)

(reg-sub
 ::history-search-bufnr
 :<- [::history-search]
 :-> :bufnr)
