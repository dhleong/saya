(ns saya.modules.scripting.layout
  (:require
   [archetype.util :refer [>evt]]
   [saya.modules.layout.events :as layout-events]
   [saya.modules.scripting.core :refer [*script-file*]]))

(defn configure
  [component-or-layout-fn]
  (let [layout-fn (if (fn? component-or-layout-fn)
                    component-or-layout-fn
                    (constantly component-or-layout-fn))]
    (>evt [::layout-events/set-current-tab-layout
           {:layout layout-fn
            :script-file *script-file*}])))
