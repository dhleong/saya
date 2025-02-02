(ns saya.modules.command.core
  (:require
   [re-frame.core :refer [reg-event-fx]]
   [saya.modules.command.interceptors :refer [aliases]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/quit
 [(aliases :q :qu :qui)]
 (fn [{:keys [db]} _]
   (cond
     (= :cmdline (:current-winnr db))
     {:db (-> db
              (assoc :current-winnr (:last-winnr db))
              (dissoc :last-winnr))}

     ; TODO: Actually this should *just* close the current window,
     ; if there are multiple
     :else
     {:fx [[:quit]]})))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/qall
 [(aliases :qa :qal)]
 (fn [_ _]
    ; TODO: Confirm, if there are active connections
   {:fx [[:quit]]}))

(comment
  (re-frame.core/dispatch [:command/quit]))
