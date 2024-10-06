(ns saya.modules.command.connection
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.config :as config]
   [saya.modules.command.interceptors :refer [aliases]]
   [saya.modules.kodachi.events :as kodachi-events]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/connect
 [(aliases :c :co :con :conn) unwrap]
 (fn [_ {[uri] :params}]
   {:dispatch [::kodachi-events/connect
               {:uri (or uri
                         (when config/debug?
                           ; TODO: Clean this up
                           "legendsofthejedi.com:5656"))}]}))
