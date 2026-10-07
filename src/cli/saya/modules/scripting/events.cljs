(ns saya.modules.scripting.events
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.echo.core :refer [echo-fx]]
   [saya.modules.kodachi.fx :as kodachi-fx]
   [saya.modules.scripting.config :refer [format-user-keymaps
                                          format-user-triggers]]))

(defn- apply-packing-errors [f & args]
  (try
    [(apply f args) nil]
    (catch :default e
      [nil e])))

(reg-event-fx
 ::reconfigure-connection
 [unwrap]
 (fn [{:keys [db]} {:keys [connection-id script-file] :as params}]
   (let [bufnr (get-in db [:connections connection-id :bufnr])
         [keymaps err1] (apply-packing-errors
                         format-user-keymaps
                         connection-id
                         (:keymaps params))
         [triggers err2] (apply-packing-errors
                          format-user-triggers
                          connection-id
                          (:triggers params))]
     {:db (cond-> db
            :always
            (-> (assoc-in [:connections connection-id :script-file] script-file)
                (assoc-in [:script-files script-file]
                          {:connection-id connection-id}))

            keymaps
            (assoc-in [:buffers bufnr :keymaps] keymaps)

            ; TODO: pass to kodachi
            triggers
            (assoc-in [:connections connection-id :triggers] triggers))
      :fx (into
           [[::kodachi-fx/configure-connection!
             {:connr connection-id
              :triggers triggers}]]

           (keep
            (fn [[what err]]
              (when err
                (echo-fx :error "Error parsing " what ": " err)))
            {"keymaps" err1
             "triggers" err2}))})))

(reg-event-fx
 ::trigger-matched
 [unwrap]
 (fn [{:keys [db]} {:keys [connr handler-id context]}]
   (when-let [trigger (get-in db [:connections connr :triggers handler-id :do])]
     {:fx [[:saya.modules.scripting.fx/call-handler
            {:f trigger
             :args [context]}]]})))
