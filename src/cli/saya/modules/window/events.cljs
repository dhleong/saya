(ns saya.modules.window.events
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-db unwrap]]))

(reg-event-db
 ::on-measured
 [unwrap]
 (fn [db {:keys [id width height]}]
   (update-in db [:windows id] merge {:width width
                                      :height height})))

(reg-event-db
 ::set-input-text
 [unwrap]
 (fn [db {:keys [connr text]}]
   ; NOTE: formatting string text like it's a buffer with :lines here:
   (assoc-in db [:buffers [:conn/input connr] :lines]
             (->> text
                  (str/split-lines)
                  (map (fn [line]
                         [{:ansi line}]))))))
