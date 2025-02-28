(ns saya.cli
  (:require
   ["ink" :as k]
   [promesa.core :as p]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [saya.cli.fullscreen :refer [activate-alternate-screen]]
   [saya.env :as env]
   [saya.events :as events]
   [saya.fx]
   [saya.subs]
   [saya.util.logging :as logging]
   [saya.util.ink :as ink]
   [saya.views :as views]

   ; NOTE: Required here just to convince shadow to build it in dev
   ; Ideally we can strip this from prod builds...
   [saya.util.ink-testing-utils]))

(defonce ^:private functional-compiler (r/create-compiler
                                        {:function-components true}))
(defonce ^:private ink-instance (atom nil))

(r/set-default-compiler! functional-compiler)

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

(defn ^:export init []
  (set! (.-title js/process) "saya")

  (p/do!
   (activate-alternate-screen
    :on-deactivate #(when-some [^js ink @ink-instance]
                      (.unmount ink)))

   (logging/patch)
   (re-frame/dispatch-sync [::events/initialize-db])
   (re-frame/dispatch-sync [::events/initialize-cli
                            js/process.argv])

   (mount-root)

   (env/initialize)))
