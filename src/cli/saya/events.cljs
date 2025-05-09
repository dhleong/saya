(ns saya.events
  (:require
   [re-frame.core :refer [path reg-event-db reg-event-fx trim-v
                          unwrap]]
   [saya.db :as db]
   [saya.modules.command.parse :refer [parse-command]]
   [saya.modules.command.registry]
   [saya.modules.echo.events]
   [saya.modules.input.keymaps :as keymaps]
   [saya.modules.input.normal :refer [scroll-to-bottom]]
   [saya.modules.kodachi.fx :as kodachi-fx]
   [saya.modules.scripting.fx :as scripting-fx]))

; NOTE: We include the whole echo module as top-level to let it
; declare the :echo event globally

(reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db}))

(reg-event-fx
 ::initialize-cli
 [trim-v]
 (fn [{:keys [db]} [cli-args]]
   (try
     (let [new-db (assoc db :cli/args cli-args)]
       {:db new-db
        :fx [[::kodachi-fx/init new-db]]})
     (catch :default e
       {:db (assoc db :err e)}))))

(reg-event-fx
 ::load-script
 [trim-v]
 (fn [_ [script-file]]
   {::scripting-fx/load-script script-file}))

(reg-event-db
 ::set-dimens
 [trim-v (path :dimens)]
 (fn [_ [width height]]
   {:width width :height height}))

(reg-event-db
 ::set-global-cursor
 [trim-v (path :cursor)]
 (fn [_ [position]]
   position))

(reg-event-fx
 :submit-raw-command
 [trim-v]
 (fn [{:keys [db]} [command-string]]
   (try
     (let [parsed (parse-command command-string)]
       (if (:event parsed)
         {:db (update db :buffers dissoc :cmd)
          :fx [[:dispatch [(:event parsed) parsed]]
               [:dispatch [:exit-command-mode]]]}

         (throw (ex-info (str "No such command: " (:command parsed))
                         {:parsed parsed}))))
     (catch :default e
       {:db (update db :buffers dissoc :cmd)
        :fx [[:dispatch [:echo :exception e]]
             [:dispatch [:exit-command-mode]]]}))))

(reg-event-db
 :exit-command-mode
 (fn [db _]
   (cond-> db
     (= :command (:mode db))
     (assoc :mode :normal))))

(reg-event-fx
 :connection/send
 [unwrap]
 (fn [{:keys [db] :as cofx} {:keys [connr text]}]
   (merge
    (let [bufnr (get-in db [:connections connr :bufnr])
          winnr (:current-winnr db)]
      ; Scroll to the bottom in the current window (only) IF it is
      ; associated with this connection
      (cond-> cofx
        (= (get-in db [:windows winnr :bufnr]) bufnr)
        (keymaps/perform scroll-to-bottom)))

    {::kodachi-fx/send! {:connection-id connr
                         :text text}})))

(reg-event-fx
 :connection/set-window-size
 [unwrap]
 (fn [_ {:keys [connr width height]}]
   {::kodachi-fx/set-window-size! {:connection-id connr
                                   :width width
                                   :height height}}))

