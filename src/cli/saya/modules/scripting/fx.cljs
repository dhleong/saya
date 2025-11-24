(ns saya.modules.scripting.fx
  (:require
   [promesa.core :as p]
   [re-frame.core :refer [reg-fx]]
   [saya.env :as env]
   [saya.modules.echo.core :refer [echo]]
   [saya.modules.logging.core :refer [log]]
   [saya.modules.scripting.callbacks :refer [trigger-callback]]
   [saya.util.paths :as paths]))

(reg-fx
 ::trigger-callback
 (fn [{:keys [connection-id callback-kind]}]
   (trigger-callback connection-id callback-kind)))

(reg-fx
 ::load-script
 (fn [script-path]
   (let [expanded-path (paths/resolve-user script-path)]
     (log "[script:load] " script-path)
     (-> (env/load-script expanded-path)
         (p/catch (fn [e]
                    (log "[script:load] ERROR: " e)
                    (echo :error "Error detected while processing" expanded-path "\n" e)))))))
