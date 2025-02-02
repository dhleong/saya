(ns saya.modules.input.events
  (:require
   [re-frame.core :refer [reg-event-db unwrap]]))

(reg-event-db
 ::set-cmdline-bufnr
 [unwrap]
 (fn [db {:keys [bufnr on-submit]}]
   (-> db
       (assoc :mode :normal) ; maybe cmdline?
       (merge {:mode :normal
               :current-winnr :cmdline
               :last-winnr (:current-winnr db)})
       (assoc-in [:buffers bufnr :cursor] {:row 0 :col 0}) ; FIXME: is this okay?
       (assoc-in [:windows :cmdline] {:id :cmdline
                                      :on-submit on-submit
                                      :bufnr bufnr}))))
