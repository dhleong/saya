(ns saya.cli
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            ["ink" :as k]
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

  ; NOTE: Somewhat hacky way to use the alternate screen:
  (letfn [(write [s]
            (js/process.stdout.write s))]
    (write "\u001b[?1049h")
    (js/process.on "SIGINT" #(do
                               (write "\u001b[?1049l"))))

  (logging/patch)
  (re-frame/dispatch-sync [::events/initialize-db])

  (mount-root))
