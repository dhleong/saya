(ns saya.modules.scripting.events
  (:require
   [re-frame.core :refer [reg-event-db unwrap]]
   [saya.modules.scripting.config :refer [format-user-keymaps]]))

(reg-event-db
 ::reconfigure-connection
 [unwrap]
 (fn [db {:keys [connection-id script-file keymaps]}]
   (let [bufnr (get-in db [:connections connection-id :bufnr])]
     (-> db
         (assoc-in [:connections connection-id :script-file] script-file)
         (assoc-in [:buffers bufnr :keymaps] (format-user-keymaps
                                              connection-id
                                              keymaps))))))
