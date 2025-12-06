(ns saya.cli
  (:require
   ["ink" :as k]
   [applied-science.js-interop :as j]
   [clojure.core.match :as m]
   [promesa.core :as p]
   [re-frame.core :as re-frame]
   [saya.cli.args :as args]
   [saya.cli.fullscreen :refer [activate-alternate-screen]]
   [saya.env :as env]
   [saya.events :as events]
   [saya.modules.input.test-helpers]
   [saya.prelude]
   [saya.reagent :as reagent]
   [saya.util.ink :as ink]
   [saya.util.ink-testing-utils] ; NOTE: Required here just to convince shadow to build them in dev
   [saya.util.logging :as logging]
   [saya.views :as views]
   [saya.modules.perf.core :as perf]))

(defonce ^:private ink-instance (atom nil))
(defonce ^:private stdout js/process.stdout)

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)

  (let [app (reagent/as-root [views/main])]
    (if-some [ink @ink-instance]
      (.rerender ^js ink app)
      (reset! ink-instance (k/render app #js {:exitOnCtrlC false
                                              :patchConsole false
                                              :stdout (ink/stdout
                                                       {} stdout)})))))

(defn- -main [args]
  (perf/init!)

  (p/do
    (activate-alternate-screen
     :stdout stdout
     :on-deactivate #(when-some [^js ink @ink-instance]
                       (ink/unmount ink)))

    (logging/patch)
    (re-frame/dispatch-sync [::events/initialize-db])
    (re-frame/dispatch-sync [::events/initialize-cli args])

    (mount-root)

    (env/initialize)

    (m/match args
      [:connect uri] (re-frame/dispatch [:command/connect {:uri uri}])
      [:load-script path] (re-frame/dispatch [::events/load-script path])

      ; No specific command
      :else nil)))

(defn ^:export init []
  (set! (.-title js/process) "saya")

  (-> (p/let [args (args/parse-cli-args js/process.argv)]
        (-main args))
      (p/catch (fn [e]
                 (println "saya: FATAL ERROR: " e "\n" (j/get e :stack "(no stack)"))
                 (js/process.exit 1)))))
