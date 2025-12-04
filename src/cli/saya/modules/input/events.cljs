(ns saya.modules.input.events
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-db unwrap]]
   [saya.config :as config]))

(reg-event-db
 ::set-cmdline-bufnr
 [unwrap]
 (fn [db {:keys [bufnr on-submit]}]
   (let [buffer (get-in db [:buffers bufnr])]
     (-> db
         (assoc :mode :normal) ; maybe cmdline?
         (merge {:mode :normal
                 :current-winnr :cmdline
                 :last-winnr (:current-winnr db)})
         (assoc-in [:buffers bufnr :cursor] {:row (-> (:lines buffer)
                                                      (count)
                                                      (dec)
                                                      (max 0))
                                             :col (get-in buffer [:cursor :col] 0)})
         (assoc-in [:windows :cmdline] {:id :cmdline
                                        :on-submit on-submit
                                        :bufnr bufnr})))))

(defn add-history-entry [history new-entry]
  (->> history
       (filter (partial not= new-entry))
       (cons new-entry)
       (take config/history-length)))

(reg-event-db
 ::add-history
 [unwrap]
 (fn [db {:keys [bufnr entry]}]
   (cond-> db
     (seq (str/trim entry))
     (update-in [:histories bufnr] add-history-entry entry))))
