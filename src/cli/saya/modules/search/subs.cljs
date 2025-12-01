(ns saya.modules.search.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::input-text
 :-> (constantly ""))

(reg-sub
 ::search
 :-> :search)

(reg-sub
 ::all-results
 :<- [::search]
 :-> :results)

(reg-sub
 ::direction
 :<- [::search]
 :-> :direction)

(reg-sub
 ::results-by-line
 :<- [::all-results]
 (fn [results [_ {:keys [bufnr start end]}]]
   (some->>
    (get results bufnr)
    (filter #(<= start (:row (:at %)) end))
    (group-by (comp :row :at)))))
