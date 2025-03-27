(ns saya.modules.logging.view
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.modules.logging.subs :as subs]))

(defn logging-view []
  (let [window-size (<sub [::subs/window-size])]
    (when (> window-size 0)
      (let [messages (<sub [::subs/recent-logs])]
        [:> k/Box {:height (min window-size (inc (count messages)))
                   :flex-direction :column
                   :overflow :hidden}
         (if (seq messages)
           (for [{:keys [timestamp text]} messages]
             ^{:key [timestamp text]}
             [:> k/Text text])
           [:> k/Text {:dim-color true}
            "(no logs yet)"])]))))
