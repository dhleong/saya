(ns saya.events
  (:require
   [re-frame.core :refer [path reg-event-db reg-event-fx trim-v unwrap]]
   [saya.cli.args :refer [parse-cli-args]]
   [saya.db :as db]
   [saya.modules.command.parse :refer [parse-command]]
   [saya.modules.command.registry]
   [saya.modules.input.keymaps :as keymaps]
   [saya.modules.input.normal :refer [scroll-to-bottom]]
   [saya.modules.kodachi.fx :as kodachi-fx]))

(reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db}))

(reg-event-fx
 ::initialize-cli
 [trim-v]
 (fn [{:keys [db]} [args]]
   (try
     (let [new-db (assoc db :cli/args (parse-cli-args args))]
       {:db new-db
        :fx [[::kodachi-fx/init new-db]]})
     (catch :default e
       {:db (assoc db :err e)}))))

(reg-event-db
 ::set-dimens
 [trim-v (path :dimens)]
 (fn [_ [width height]]
   {:width width :height height}))

(reg-event-fx
 :submit-raw-command
 [trim-v]
 (fn [{:keys [db]} [command-string]]
   (try
     (let [parsed (parse-command command-string)]
       (if (:event parsed)
         {:db (update db :buffers dissoc :cmd)
          :fx [[:dispatch [(:event parsed) parsed]]
               [:dispatch [:exit-command-mode]]]}

         (throw (ex-info (str "No such command: " (:command parsed))
                         {:parsed parsed}))))
     (catch :default e
        ; TODO: echo
       {:db (update db :buffers dissoc :cmd)
        :fx [[:log (str e)]
             [:dispatch [:exit-command-mode]]]}))))

(reg-event-db
 :exit-command-mode
 (fn [db _]
   (cond-> db
     (= :command (:mode db))
     (assoc :mode :normal))))

(reg-event-fx
 :connection/send
 [unwrap]
 (fn [{:keys [db] :as cofx} {:keys [connr text]}]
   (merge
    (let [bufnr (get-in db [:connection->bufnr connr])
          winnr (:current-winnr db)]
      ; Scroll to the bottom in the current window (only) IF it is
      ; associated with this connection
      (cond-> cofx
        (= (get-in db [:windows winnr :bufnr]) bufnr)
        (keymaps/perform scroll-to-bottom)))

    {::kodachi-fx/send! {:connection-id connr
                         :text text}})))
