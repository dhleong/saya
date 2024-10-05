(ns saya.modules.kodachi.events
  (:require
   [clojure.core.match :as m]
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-db reg-event-fx trim-v unwrap]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.logging.core :refer [log]]))

(reg-event-db
 ::initializing
 (fn [db _]
   (assoc db :kodachi :initializing)))

(reg-event-db
 ::ready
 (fn [db _]
   (assoc db :kodachi :ready)))

(reg-event-db
 ::unavailable
 [trim-v]
 (fn [db [err]]
   (-> db
       (assoc :kodachi :unavailable)
       (assoc :kodachi/err err))))

(reg-event-fx
 ::on-error
 [trim-v]
 (fn [_ [_err]]
    ; TODO: Cleanup any connection state, etc.
    ; TODO: Echo an error message
   {:fx [[:saya.modules.kodachi.fx/init]]}))

(reg-event-fx
 ::connecting
 [unwrap]
 (fn [_ params]
    ; TODO: Stash connection state somewhere?
   {:fx [[:dispatch
          [::buffer-events/create-for-connection params]]]}))

(reg-event-fx
 ::on-message
 [unwrap]
 (fn [{:keys [db]} {:keys [connection_id] :as params}]
   (log "<< " params)
   (when-let [bufnr (get-in db [:connection->bufnr connection_id])]
     (m/match params
       {:type "ExternalUI"
        :data {:type "Text"
               :ansi ansi}}
       {:dispatch [::buffer-events/append-text
                   {:id bufnr
                    :ansi (str/trim-newline ansi)}]}

       {:type "ExternalUI"
        :data {:type "NewLine"}}
       {:dispatch [::buffer-events/new-line
                   {:id bufnr}]}

       ; TODO:
       :else
       nil))))

(reg-event-fx
 ::connect
 [unwrap]
 (fn [_ {:keys [uri]}]
   {:saya.modules.kodachi.fx/connect! {:uri uri}}))

(comment
  (re-frame.core/dispatch [::connect {:uri "legendsofthejedi.com:5656"}])
  (re-frame.core/dispatch [::connect {:uri "procrealms.ddns.net:3000"}]))
