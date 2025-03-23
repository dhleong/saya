(ns saya.modules.command.registry.buffer
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.command.interceptors :refer [aliases]]
   [saya.modules.echo.core :refer [echo-fx]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/enew
 [(aliases :ene) unwrap]
 (fn [{:keys [db]} _]
   (if (some? (:current-winnr db))
     ; TODO: actually we *should* do this IF unsaved
     {:fx [(echo-fx :error "Unable to :enew with an active buffer")]}

     {:db (let [[db _] (buffer-events/create-blank db)]
            db)})))
