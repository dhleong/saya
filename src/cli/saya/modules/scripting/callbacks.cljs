(ns saya.modules.scripting.callbacks)

(defonce ^:private registered-callbacks (atom {}))

(defn register-callback [connr callback]
  (swap! registered-callbacks assoc connr callback))

(defn trigger-callback [connr callback-kind]
  (when-some [callback (get @registered-callbacks connr)]
    (callback callback-kind))
  (when (#{:on-error :on-disconnected} callback-kind)
    ; Cleanup
    (swap! registered-callbacks dissoc connr)))
