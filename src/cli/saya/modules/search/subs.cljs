(ns saya.modules.search.subs
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-sub]]
   [saya.modules.buffers.subs :as buffer-subs]))

(reg-sub
 ::input-text
 :<- [::buffer-subs/by-id :search]
 (fn [buffer]
   ; NOTE: There should be only one, if any
   (or (some->> (:lines buffer)
                (str/join "\n"))
       "")))

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
