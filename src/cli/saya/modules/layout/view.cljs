(ns saya.modules.layout.view
  (:require
   [archetype.util :refer [<sub]]
   [saya.modules.layout.subs :as subs]))

(defn layout-view [tab-id]
  (let [layout (<sub [::subs/evaluated tab-id])]
    ; Render it directly :shock:
    layout))
