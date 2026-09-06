(ns saya.modules.home.core
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.command.view :refer [command-line-mode-view]]
   [saya.modules.echo.view :refer [echo-window]]
   [saya.modules.input.cmdline :refer [cmdline-window]]
   [saya.modules.kodachi.subs :as kodachi]
   [saya.modules.layout.subs :as layout-subs]
   [saya.modules.layout.view :refer [layout-view]]
   [saya.modules.logging.view :refer [logging-view]]
   [saya.modules.search.view :refer [search-mode-view]]
   [saya.modules.ui.error-boundary :refer [error-boundary]]
   [saya.modules.ui.placeholders :as placeholders]
   [saya.modules.window.view :refer [window-view]]))

(defn- home-content []
  (let [layout (<sub [::layout-subs/current])
        current-winnr (<sub [:current-winnr])]
    (cond
      (some? layout)
      [:> k/Box {:flex-direction :column
                 :height :100%
                 :width :100%
                 :justify-content :center
                 :align-items :center}
       [error-boundary
        ; TODO: tab id?
        [layout-view 0]]]

      ; HACKS:
      (some? current-winnr)
      [window-view (or (when (number? current-winnr)
                         current-winnr)
                       (<sub [:last-winnr]))]

      :else
      [:> k/Box {:flex-direction :column
                 :height :100%
                 :width :100%
                 :justify-content :center
                 :align-items :center}
       [:> k/Text "Welcome to saya"]

       (case (<sub [::kodachi/state])
         :unavailable [:> k/Text "Could not locate or install kodachi"]
         :initializing [:> k/Text "..."]
         (nil :ready) nil)])))

(defn- status-area []
  (let [mode (<sub [:mode])
        has-echo? (seq (<sub [:echo-lines]))]
    [:<>
     (case mode
       :command [command-line-mode-view]
       :search [search-mode-view]
       (when-not has-echo?
         [placeholders/line]))
     [echo-window]]))

(defn home-view []
  [:> k/Box {:flex-direction :column
             :height :100%
             :width :100%}
   [logging-view]

   [:> k/Box {:flex-grow 1
              :width :100%}
    [error-boundary
     [home-content]]]

   [:> k/Box {:align-self :bottom
              :flex-direction :column
              :width :100%}
    [cmdline-window]
    [status-area]]])
