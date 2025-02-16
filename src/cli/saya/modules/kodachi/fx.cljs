(ns saya.modules.kodachi.fx
  (:require
   [archetype.util :refer [>evt]]
   [promesa.core :as p]
   [re-frame.core :refer [reg-fx]]
   [saya.modules.kodachi.api :as api]
   [saya.modules.kodachi.events :as events]
   [saya.modules.logging.core :refer [log]]))

(reg-fx
 ::init
 (fn [_db]
   (>evt [::events/initializing])

   (-> (api/init)
       (p/catch (fn [e]
                  (>evt [::events/unavailable e]))))))

(reg-fx
 ::connect!
 (fn [{:keys [uri]}]
   (p/let [{:keys [connection_id]} (api/request! {:type :Connect
                                                  :uri uri})]
     (log "Opened connection" connection_id "to" uri)
     (>evt [::events/connecting {:uri uri
                                 :connection-id connection_id}])
     (log "Queued ::connecting"))))

(reg-fx
 ::disconnect!
 (fn [{:keys [connection-id]}]
   (api/request! {:type :Disconnect
                  :connection_id connection-id})))

(reg-fx
 ::send!
 (fn [{:keys [connection-id text]}]
   (api/request! {:type :Send
                  :connection_id connection-id
                  :text text})))

(reg-fx
 ::set-window-size!
 (fn [{:keys [connection-id width height]}]
   (api/dispatch! {:type :WindowSize
                   :connection_id connection-id
                   :width width
                   :height height})))
