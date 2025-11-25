(ns saya.cli
  (:require ; NOTE: Required here just to convince shadow to build them in dev
 ; Ideally we can strip these from prod builds...
   [clojure.core.match :as m]
   ["ink" :as k] ; NOTE: Required here just to convince shadow to build them in dev
   [promesa.core :as p]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [saya.cli.args :as args]
   [saya.cli.fullscreen :refer [activate-alternate-screen]]
   [saya.env :as env]
   [saya.events :as events]
   [saya.modules.input.test-helpers]
   [saya.modules.ui.cursor :refer [get-cursor-shape]]
   [saya.prelude]
   [saya.util.ink :as ink]
   [saya.util.ink-testing-utils]
   [saya.util.logging :as logging]
   [saya.views :as views] ; NOTE: Required here just to convince shadow to build them in dev
   ))

(defonce ^:private ink-instance (atom nil))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)

  (let [app (r/as-element [views/main])]
    ; NOTE: This hack is from gakki to fix hot reloads. They
    ; *mostly* work without, but... still not totally consistent,
    ; and this will only matter in dev anyway.
    (when-let [^js ink @ink-instance]
      (.clear ink)
      (.unmount ink))

    (reset! ink-instance (k/render app #js {:exitOnCtrlC false
                                            :stdout (ink/stdout)}))))

(defn- -main [args]
  (p/do
    (activate-alternate-screen
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
                 (println "saya: FATAL ERROR: " e "\n" (.-stack e))
                 (js/process.exit 1)))))
