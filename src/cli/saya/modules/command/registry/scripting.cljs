(ns saya.modules.command.registry.scripting
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.command.interceptors :refer [aliases]]
   [saya.modules.scripting.fx :as scripting-fx]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/reload
 [(aliases :r :re :rel) unwrap]
 (fn [{:keys [db]} _]
   (let [winnr (:current-winnr db)
         bufnr (get-in db [:windows winnr :bufnr])
         connr (get-in db [:buffers bufnr :connection-id])
         script-file (get-in db [:connections connr :script-file])]
     (cond
       (not connr)
       {:dispatch [:echo :error "No connection associated with current buffer."]}

       (not script-file)
       {:dispatch [:echo :error "No script associated with current connection."]}

       :else
       {::scripting-fx/load-script script-file}))))
