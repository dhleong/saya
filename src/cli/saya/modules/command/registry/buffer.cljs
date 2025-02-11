(ns saya.modules.command.registry.buffer
  (:require
   [day8.re-frame-10x.inlined-deps.re-frame.v1v3v0.re-frame.core :refer [unwrap]]
   [re-frame.core :refer [reg-event-fx]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.command.interceptors :refer [aliases]]
   [saya.modules.logging.core :refer [log-fx]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/enew
 [(aliases :ene) unwrap]
 (fn [{:keys [db]} _]
   (if (some? (:current-winnr db))
     ; TODO: echo errors + actually we *should* do this if unsaved
     {:fx [(log-fx "Unable to :enew with an active buffer")]}

     {:db (let [[db _] (buffer-events/create-blank db)]
            db)})))
