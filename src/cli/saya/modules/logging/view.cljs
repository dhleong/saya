(ns saya.modules.logging.view
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.logging.subs :as subs]))

(defn logging-view []
  (let [messages (<sub [::subs/recent-logs])]
    [:> k/Box {:height (min 2 (count messages))
               :flex-direction :column
               :overflow :hidden}
     (for [{:keys [timestamp text]} messages]
       ^{:key [timestamp text]}
       [:> k/Text text])]))
