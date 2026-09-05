(ns saya.modules.command.registry.scripting
  (:require
   [clojure.core.match :as m]
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.command.interceptors :refer [aliases]]
   [saya.modules.scripting.fx :as scripting-fx]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/reload
 [(aliases :r :re :rel) unwrap]
 (fn [{:keys [db]} _]
   (let [winnr (:current-winnr db)
         layout-nr (:current-layout db 0)
         bufnr (get-in db [:windows winnr :bufnr])
         connr (get-in db [:buffers bufnr :connection-id])
         layout-script-file (get-in db [:layouts layout-nr :script-file])
         conn-script-file (get-in db [:connections connr :script-file])
         script-file (or conn-script-file layout-script-file)]
     (m/match [{:conn? (some? connr)
                :script? (some? script-file)}]
       [{:script? true}] {::scripting-fx/load-script script-file}
       [{:script? false :conn? false}] {:dispatch [:echo :error "No script associated with current buffer."]}
       [{:script? false :conn? true}] {:dispatch [:echo :error "No script associated with current connection."]}))))
