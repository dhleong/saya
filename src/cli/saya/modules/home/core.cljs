(ns saya.modules.home.core
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.buffers.view :refer [buffer-view]]
   [saya.modules.command.view :refer [command-line-mode-view]]
   [saya.modules.kodachi.subs :as kodachi]
   [saya.modules.logging.view :refer [logging-view]]))

(defn- home-content []
  [:> k/Box {:flex-direction :column
             :height :100%
             :width :100%
             :justify-content :center
             :align-items :center}
   [:> k/Text "Welcome to saya"]

   (case (<sub [::kodachi/state])
     :unavailable [:> k/Text "Could not locate or install kodachi"]
     :initializing [:> k/Text "..."]
     (nil :ready) nil)

   [buffer-view 0]])

(defn- status-area []
  (let [mode (<sub [:mode])]
    (case mode
      :command [command-line-mode-view]

      ; Default:
      [:> k/Text "Status"])))

(defn home-view []
  [:> k/Box {:flex-direction :column
             :height :100%
             :width :100%}
   ; TODO: Eventually, hide this by default
   [logging-view]

   [:> k/Box {:flex-grow 1
              :width :100%}
    [home-content]]

   [:> k/Box {:align-self :bottom
              :width :100%}
    [status-area]]])
