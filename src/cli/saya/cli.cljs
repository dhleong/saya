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
    ; NOTE: This hack was from gakki to fix hot reloads. Keeping
    ; it around in case it's still necessary...
    #_(when-let [^js instance @ink-instance]
        (.clear instance)
        (.unmount instance))

    (if-let [^js instance @ink-instance]
      (.rerender instance app)

      (reset! ink-instance (k/render app)))))

(defn ^:export init []
  (set! (.-title js/process) "saya")

  (p/do!
   (activate-alternate-screen
    :on-deactivate #(when-some [ink @ink-instance]
                      (.unmount ink)))

   (logging/patch)
   (re-frame/dispatch-sync [::events/initialize-db])

   (mount-root)))
