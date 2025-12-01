(ns saya.modules.command.registry.core
  (:require
   [re-frame.core :refer [reg-event-fx]]
   [saya.modules.command.interceptors :refer [aliases]]
   [saya.modules.echo.events :as echo-events]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/messages
 [(aliases :mes :mess)]
 (fn [_ _]
   {:dispatch [::echo-events/reveal-latest]}))

(defn- filter-buffer-to-current-line [buffer]
  (let [current-linenr (get-in buffer [:cursor :row])]
    (-> buffer
        (assoc :lines [(nth (:lines buffer) current-linenr)])
        (assoc-in [:cursor :row] 0))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-fx
 :command/quit
 [(aliases :q :qu :qui)]
 (fn [{:keys [db]} _]
   (cond
     (= :cmdline (:current-winnr db))
     {:db (let [cmdline-bufnr (get-in db [:windows :cmdline :bufnr])]
            (-> db
                (assoc :current-winnr (:last-winnr db))
                (dissoc :last-winnr)
                (update-in [:buffers cmdline-bufnr] filter-buffer-to-current-line)))}

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
