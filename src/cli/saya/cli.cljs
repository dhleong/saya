(ns saya.cli
  (:require
   ["ink" :as k]
   [promesa.core :as p]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [saya.cli.fullscreen :refer [activate-alternate-screen]]
   [saya.events :as events]
   [saya.subs]
   [saya.util.logging :as logging]
   [saya.views :as views]))

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

    (reset! ink-instance (k/render app))))

(defn ^:export init []
  (set! (.-title js/process) "saya")

  (p/do!
   (activate-alternate-screen
    :on-deactivate #(when-some [^js ink @ink-instance]
                      (.unmount ink)))

   (logging/patch)
   (re-frame/dispatch-sync [::events/initialize-db])

   (mount-root)))
