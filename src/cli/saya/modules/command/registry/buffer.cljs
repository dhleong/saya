(ns saya.modules.command.registry.buffer
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]
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
