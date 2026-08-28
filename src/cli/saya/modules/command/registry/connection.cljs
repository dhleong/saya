(ns saya.modules.command.registry.connection
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.config :as config]
   [saya.modules.command.interceptors :refer [aliases with-buffer-context]]
   [saya.modules.kodachi.events :as kodachi-events]
   [saya.util.string :as string]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/connect
 [(aliases :c :co :con :conn) unwrap]
 (fn [_ {[uri-param] :params :keys [uri auto-prompts persist-output
                                    script-file]}]
   ; NOTE: This may be invoked either as:
   ;   [:command/connect {:uri uri}]
   ; OR, as from the UI:
   ;   [:command/connect {:params [uri]}]
   (let [the-uri (or uri
                     uri-param
                     (when config/debug?
                       ; TODO: Clean this up
                       "legendsofthejedi.com:5656"))]
     {:dispatch [::kodachi-events/connect
                 {:uri the-uri
                  :auto_prompts auto-prompts
                  :persisted_output_key (when persist-output
                                          (string/slugify
                                           (str script-file
                                                "-"
                                                the-uri)))}]})))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/disconnect
 [(aliases :dis :disc :disco) unwrap with-buffer-context]
 (fn [{:keys [connr]} _]
   (if connr
     {:dispatch [::kodachi-events/disconnect
                 {:connection-id connr}]}

     {:dispatch [:echo :error "No active connection in current buffer"]})))

(comment
  (re-frame.core/dispatch [:command/connect {:params ["starmourn.com:3000"]}]))
