(ns saya.modules.home.core
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.command.view :refer [command-line-mode-view]]
   [saya.modules.input.cmdline :refer [<cmdline-window]]
   [saya.modules.kodachi.subs :as kodachi]
   [saya.modules.logging.view :refer [logging-view]]
   [saya.modules.ui.error-boundary :refer [error-boundary]]
   [saya.modules.ui.placeholders :as placeholders]
   [saya.modules.window.view :refer [window-view]]))

(defn- home-content []
  (if-let [winnr (<sub [:current-winnr])]
    [window-view winnr]

    [:> k/Box {:flex-direction :column
               :height :100%
               :width :100%
               :justify-content :center
               :align-items :center}
     [:> k/Text "Welcome to saya"]

     (case (<sub [::kodachi/state])
       :unavailable [:> k/Text "Could not locate or install kodachi"]
       :initializing [:> k/Text "..."]
       (nil :ready) nil)]))

(defn- status-area []
  (let [mode (<sub [:mode])]
    (case mode
      :command [command-line-mode-view]

      ; Default:
      [placeholders/line])))

(defn home-view []
  [:> k/Box {:flex-direction :column
             :height :100%
             :width :100%}
   ; TODO: Eventually, hide this by default
   [logging-view]

   [:> k/Box {:flex-grow 1
              :width :100%}
    [error-boundary
     [home-content]]]

   [:> k/Box {:align-self :bottom
              :flex-direction :column
              :flex-shrink 0
              :width :100%}
    [<cmdline-window]
    [status-area]]])
