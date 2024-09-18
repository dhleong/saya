(ns saya.modules.logging.view
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.logging.subs :as subs]))

(defn logging-view []
  (let [messages (<sub [::subs/recent-logs])]
    [:> k/Box {:max-height 2
               :flex-direction :column}
     (for [{:keys [timestamp text]} messages]
       ^{:key timestamp}
       [:> k/Text text])]))
