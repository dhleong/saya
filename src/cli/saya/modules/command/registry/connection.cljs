(ns saya.modules.command.registry.connection
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.config :as config]
   [saya.modules.command.interceptors :refer [aliases with-buffer-context]]
   [saya.modules.kodachi.events :as kodachi-events]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/connect
 [(aliases :c :co :con :conn) unwrap]
 (fn [_ {[uri-param] :params :keys [uri]}]
   ; NOTE: This may be invoked either as:
   ;   [:command/connect {:uri uri}]
   ; OR, as from the UI:
   ;   [:command/connect {:params [uri]}]
   {:dispatch [::kodachi-events/connect
               {:uri (or uri
                         uri-param
                         (when config/debug?
                           ; TODO: Clean this up
                           "legendsofthejedi.com:5656"))}]}))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/disconnect
 [(aliases :dis :disc :disco) unwrap with-buffer-context]
 (fn [{:keys [connr]} _]
   (if connr
     {:dispatch [::kodachi-events/disconnect
                 {:connection-id connr}]}

     ; TODO: echo
     {:dispatch [:log "No active connection in current buffer"]})))

(comment
  (re-frame.core/dispatch [:command/connect {:params ["starmourn.com:3000"]}]))
