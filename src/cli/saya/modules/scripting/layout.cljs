(ns saya.modules.scripting.layout
  (:require
   [archetype.util :refer [>evt]]
   [saya.modules.layout.events :as layout-events]))

(defn configure
  ([static-layout] (configure (atom nil) (constantly static-layout)))
  ([state-atom layout-fn]
   (>evt [::layout-events/set-current-tab-layout
          {:layout layout-fn
           :state-atom state-atom}])))
