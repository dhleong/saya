(ns saya.modules.scripting.events
  (:require
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.echo.core :refer [echo-fx]]
   [saya.modules.scripting.config :refer [format-user-keymaps]]))

(reg-event-fx
 ::reconfigure-connection
 [unwrap]
 (fn [{:keys [db]} {:keys [connection-id script-file keymaps]}]
   (let [bufnr (get-in db [:connections connection-id :bufnr])
         [keymaps err] (try
                         [(format-user-keymaps
                           connection-id
                           keymaps)
                          nil]
                         (catch :default e
                           [nil e]))]
     {:db (cond-> db
            :always
            (assoc-in [:connections connection-id :script-file] script-file)

            keymaps
            (assoc-in [:buffers bufnr :keymaps] keymaps))
      :fx [(when err
             (echo-fx :error "Error parsing keymaps: " err))]})))
