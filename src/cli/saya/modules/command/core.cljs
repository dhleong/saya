(ns saya.modules.command.core
  (:require
   [re-frame.core :refer [reg-event-fx]]
   [saya.modules.command.interceptors :refer [aliases]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/quit
 [(aliases :q :qu :qui)]
 (fn [_ _]
    ; TODO: Actually this should *just* close the current buffer,
    ; if there are multiple
   {:fx [[:quit]]}))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/qall
 [(aliases :qa :qal)]
 (fn [_ _]
    ; TODO: Confirm, if there are active connections
   {:fx [[:quit]]}))

(comment
  (re-frame.core/dispatch [:command/quit]))
