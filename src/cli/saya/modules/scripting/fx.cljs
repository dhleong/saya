(ns saya.modules.scripting.fx
  (:require
   [promesa.core :as p]
   [re-frame.core :refer [reg-fx]]
   [saya.env :as env]
   [saya.modules.echo.core :refer [echo]]
   [saya.modules.logging.core :refer [log]]
   [saya.modules.scripting.callbacks :refer [trigger-callback]]
   [saya.util.paths :as paths]
   [saya.modules.kodachi.api :as api]))

(reg-fx
 ::call-handler
 (fn [{:keys [f args]}]
   (js/setImmediate
    (fn []
      (try
        (apply f args)
        (catch :default e
          (echo :error "Error invoking handler:" e)))))))

(reg-fx
 ::process-alias-handler
 (fn [{:keys [f args handler-id request-id]}]
   (js/setImmediate
    (fn []
      (-> (p/let [result (apply f args)]
            (api/dispatch!
             {:type :AliasMatchHandled
              :request-id request-id
              :hander-id handler-id
              :replacement (if (string? result)
                             result
                             "")}))
          (p/catch (fn [e]
                     (echo :error "Error invoking alias handler:" e))))))))

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
         (p/then (fn [_]
                   (echo "Loaded" expanded-path)))
         (p/catch (fn [e]
                    (log "[script:load] ERROR: " e)
                    (echo :error "Error detected while processing" expanded-path "\n" e)))))))
