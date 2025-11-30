(ns saya.modules.input.events
  (:require
   [re-frame.core :refer [reg-event-db unwrap]]
   [saya.config :as config]))

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

(defn add-history-entry [history new-entry]
  (->> history
       (filter (partial not= new-entry))
       (cons new-entry)
       (take config/history-length)))

(reg-event-db
 ::add-history
 [unwrap]
 (fn [db {:keys [bufnr entry]}]
   (update-in db [:histories bufnr] add-history-entry entry)))
