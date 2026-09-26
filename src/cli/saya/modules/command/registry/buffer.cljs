(ns saya.modules.command.registry.buffer
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.buffers.fx :as buffers-fx]
   [saya.modules.buffers.util :as buffers]
   [saya.modules.command.interceptors :refer [aliases with-buffer-context]]
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

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/write
 [(aliases :w) with-buffer-context unwrap]
 (fn [{:keys [bufnr connr db]} _]
   (let [buffer (get-in db [:buffers bufnr])]
     (cond
       (some? connr)
       {:fx [(echo-fx :exception "Invalid in a connection window")]}

       (buffers/readonly? buffer)
       {:fx [(echo-fx :exception "Invalid in a read-only buffer")]}

       ; TODO: We could accept a filename arg?
       (not (:file-path buffer))
       {:fx [(echo-fx :exception "No file associated with buffer")]}

       :else
       {:fx [[::buffers-fx/write buffer]]}))))
