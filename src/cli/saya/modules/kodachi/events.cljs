(ns saya.modules.kodachi.events
  (:require
   [clojure.core.match :as m]
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-db reg-event-fx trim-v unwrap]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.logging.core :refer [log]]))

(reg-event-db
 ::initializing
 (fn [db _]
   (assoc db :kodachi :initializing)))

(reg-event-db
 ::ready
 (fn [db _]
   (assoc db :kodachi :ready)))

(reg-event-db
 ::unavailable
 [trim-v]
 (fn [db [err]]
   (-> db
       (assoc :kodachi :unavailable)
       (assoc :kodachi/err err))))

(reg-event-fx
 ::on-error
 [trim-v]
 (fn [_ [err]]
    ; TODO: Cleanup any connection state, etc.
   {:fx [[:saya.modules.kodachi.fx/init]
         [:dispatch
          [:echo :exception "Kodachi error: " err]]]}))

(reg-event-fx
 ::connecting
 [unwrap]
 (fn [{:keys [db]} {:keys [connection-id uri] :as params}]
   (let [db' (buffer-events/create-for-connection db params)
         bufnr (get-in db' [:connections connection-id :bufnr])]
     {:db db'
      :dispatch [::buffer-events/new-line
                 {:id bufnr
                  :system [:connecting uri]}]})))

(defn- process-message [db connr bufnr params]
  (m/match params
    {:type "ExternalUI"
     :data {:type "Text"
            :ansi ansi}}
    {:dispatch [::buffer-events/append-text
                (let [ansi' (str/trim-newline ansi)]
                  {:id bufnr
                   :full-line? (not= ansi' ansi)
                   :ansi ansi'})]}

    {:type "ExternalUI"
     :data {:type "LocalSend"
            :text text}}
    {:fx [[:dispatch
           [::buffer-events/append-text
            {:id bufnr
             :system [:local-send text]
             :full-line? true}]]
          [:dispatch
           [::buffer-events/new-line
            {:id bufnr}]]]}

    {:type "ExternalUI"
     :data {:type "NewLine"}}
    {:dispatch [::buffer-events/new-line
                {:id bufnr}]}

    {:type "ExternalUI"
     :data {:type "ClearPartialLine"}}
    {:dispatch [::buffer-events/clear-partial-line
                {:id bufnr}]}

  ; NOTE: "Connecting" is usually handled in its explicit event handler
  ; due to timing issues between when that completes and we receive it
  ; here.

    {:type "Connected"}
    {:db (cond-> db
           :always
           (assoc-in [:connections connr :state] :connected)

           (seq js/process.env.REPLAY_DUMP)
           (assoc-in [:tti :replay/connection :start] (js/performance.now)))
     :fx [[:dispatch
           [::buffer-events/new-line
            {:id bufnr
             :system [:connected (get-in db [:buffers bufnr :uri])]}]]

        ; TODO: Perhaps, send the "largest" window for this buffer?
          (when-let [window (->> db :windows vals
                                 (filter #(= bufnr (:bufnr %)))
                                 (first))]
            [:saya.modules.kodachi.fx/set-window-size!
             {:connection-id connr
              :width (:width window)
              :height (:height window)}])

          [:saya.modules.scripting.fx/trigger-callback
           {:connection-id connr
            :callback-kind :on-connected}]]}

    {:type "Disconnected"}
    {:db (assoc-in db [:connections connr :state] :disconnected)
     :dispatch [::buffer-events/new-line
                {:id bufnr
                 :system [:disconnected (get-in db [:buffers bufnr :uri])]}]
     :fx [[:saya.modules.scripting.fx/trigger-callback
           {:connection-id connr
            :callback-kind :on-disconnected}]]}

    {:type "PromptUpdated"}
    {:db (assoc-in db [:connections connr :prompts
                       (:group_id params) (:index params)]
                   (get-in params [:content :ansi]))}

  ; TODO:
    :else
    nil))

(reg-event-fx
 ::on-message
 [unwrap]
 (fn [{:keys [db]} {connr :connection_id :as params}]
   (log "<< " params)
   (let [bufnr (get-in db [:connections connr :bufnr])]
     (cond
       (some? bufnr)
       (process-message db connr bufnr params)))))

(reg-event-fx
 ::connect
 [unwrap]
 (fn [_ payload]
   {:saya.modules.kodachi.fx/connect! payload}))

(reg-event-fx
 ::disconnect
 [unwrap]
 (fn [_ {:keys [connection-id]}]
   {:saya.modules.kodachi.fx/disconnect! {:connection-id connection-id}}))

(comment
  (re-frame.core/dispatch [::connect {:uri "legendsofthejedi.com:5656"}])
  (re-frame.core/dispatch [::connect {:uri "procrealms.ddns.net:3000"}]))
