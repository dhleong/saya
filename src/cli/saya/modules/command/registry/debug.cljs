(ns saya.modules.command.registry.debug
  (:require
   [re-frame.core :refer [reg-event-db]]))

(def ^:private default-log-window-size 5)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(reg-event-db
 :command/log
 (fn [db _]
   (let [preferred-size default-log-window-size]
     (update db :log-window-size (fn [v]
                                   (when-not (= v preferred-size)
                                     preferred-size))))))

