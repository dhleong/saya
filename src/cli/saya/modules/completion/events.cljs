(ns saya.modules.completion.events
  (:require
   [re-frame.core :refer [reg-event-db trim-v unwrap]]))

(reg-event-db
 ::start
 [unwrap]
 (fn [db {:keys [bufnr word-to-complete]}]
   (-> db
       (assoc-in [:buffers bufnr :completion :word-to-complete] word-to-complete)
       (update-in [:buffers bufnr :completion] dissoc :applied-candidate))))

(reg-event-db
 ::on-applied-candidate
 [unwrap]
 (fn [db {:keys [bufnr candidate]}]
   (assoc-in db [:buffers bufnr :completion :applied-candidate] candidate)))

(reg-event-db
 ::on-candidates
 [unwrap]
 (fn [db {:keys [bufnr candidates]}]
   (assoc-in db [:buffers bufnr :completion :candidates] candidates)))

(reg-event-db
 ::on-error
 [unwrap]
 (fn [db {:keys [bufnr _error]}]
   (update-in db [:buffers bufnr :completion] dissoc :candidates)))

(reg-event-db
 ::set-bufnr
 [trim-v]
 (fn [db [bufnr]]
   (assoc-in db [:completion :current-bufnr] bufnr)))

(reg-event-db
 ::unset-bufnr
 [trim-v]
 (fn [db [bufnr]]
   (update-in db [:completion :current-bufnr] (fn [v]
                                                (if (= v bufnr)
                                                  nil
                                                  v)))))
