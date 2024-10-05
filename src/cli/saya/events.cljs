(ns saya.events
  (:require
   [re-frame.core :refer [path reg-event-db reg-event-fx trim-v]]
   [saya.cli.args :refer [parse-cli-args]]
   [saya.db :as db]
   [saya.modules.command.parse :refer [parse-command]]
   [saya.modules.kodachi.fx :as kodachi-fx]
   [saya.modules.command.registry]))

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
 (fn [_ [command-string]]
   (try
     (let [parsed (parse-command command-string)]
       (if (:event parsed)
         {:fx [[:dispatch [(:event parsed) parsed]]
               [:dispatch [:exit-command-mode]]]}

         (throw (ex-info (str "No such command: " (:command parsed))
                         {:parsed parsed}))))
     (catch :default e
        ; TODO: echo
       {:fx [[:log (str e)]
             [:dispatch [:exit-command-mode]]]}))))

(reg-event-db
 :exit-command-mode
 (fn [db _]
   (cond-> db
     (= :command (:mode db))
     (assoc :mode :normal))))
