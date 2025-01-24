(ns saya.modules.kodachi.events
  (:require
   ["anser" :default Anser]
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
 (fn [{:keys [db]} {:keys [connection-id uri] :as params}]
   ; TODO: Stash connection state somewhere?
   (let [db (buffer-events/create-for-connection db params)
         bufnr (get-in db [:connection->bufnr connection-id])]
     {:db db
      :dispatch [::buffer-events/new-line
                 {:id bufnr
                  :system [:connecting uri]}]})))

(reg-event-fx
 ::on-message
 [unwrap]
 (fn [{:keys [db]} {connr :connection_id :as params}]
   (log "<< " params)
   (when-let [bufnr (get-in db [:connection->bufnr connr])]
     (m/match params
       {:type "ExternalUI"
        :data {:type "Text"
               :ansi ansi}}
       {:dispatch [::buffer-events/append-text
                   (let [ansi' (str/trim-newline ansi)]
                     {:id bufnr
                      :parsed ((.-ansiToJson Anser) ansi')
                      :full-line? (not= ansi' ansi)
                      :ansi ansi'})]}

       {:type "ExternalUI"
        :data {:type "NewLine"}}
       {:dispatch [::buffer-events/new-line
                   {:id bufnr}]}

       {:type "ExternalUI"
        :data {:type "ClearPartialLine"}}
       {:dispatch [::buffer-events/clear-partial-line
                   {:id bufnr}]}

       ; NOTE: "Connecting" is handled in its explicit event handler
       ; due to timing issues between when that completes
       ; and we receive it here.

       {:type "Disconnected"}
       {:dispatch [::buffer-events/new-line
                   {:id bufnr
                    :system [:disconnected]}]}

       ; TODO:
       :else
       nil))))

(reg-event-fx
 ::connect
 [unwrap]
 (fn [_ {:keys [uri]}]
   {:saya.modules.kodachi.fx/connect! {:uri uri}}))

(reg-event-fx
 ::disconnect
 [unwrap]
 (fn [_ {:keys [connection-id]}]
   {:saya.modules.kodachi.fx/disconnect! {:connection-id connection-id}}))

(comment
  (re-frame.core/dispatch [::connect {:uri "legendsofthejedi.com:5656"}])
  (re-frame.core/dispatch [::connect {:uri "procrealms.ddns.net:3000"}]))
