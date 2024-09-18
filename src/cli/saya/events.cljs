(ns saya.events
  (:require
   [re-frame.core :refer [path reg-event-db reg-event-fx trim-v]]
   [saya.db :as db]
   [saya.modules.command.registry]
   [saya.modules.command.parse :refer [parse-command]]))

(reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db}))

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
