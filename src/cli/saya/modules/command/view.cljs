(ns saya.modules.command.view
  (:require
   ["ink" :as k]
   ["react" :as React]
   [archetype.util :refer [>evt]]
   [saya.cli.text-input :refer [text-input]]
   [saya.modules.ui.cursor :refer [cursor]]))

(defn- f>command-line-mode-view []
  (let [[input set-input!] (React/useState "")]
    [:> k/Box {:flex-direction :row}
     [:> k/Text ":"
      [text-input {:on-change set-input!
                   :on-submit #(>evt [:submit-raw-command %])
                   :value input}]]
     [cursor :pipe]]))

(defn command-line-mode-view []
  [:f> f>command-line-mode-view])
