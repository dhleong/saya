(ns saya.modules.window.events
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-db reg-event-fx unwrap]]))

(reg-event-fx
 ::on-measured
 [unwrap]
 (fn [{:keys [db]} {:keys [id width height]}]
   (let [{:keys [bufnr]} (get-in db [:windows id])
         connr (get-in db [:buffers bufnr :connection-id])]
     {:db (update-in db [:windows id] merge {:width width
                                             :height height})
      :fx [(when connr
             [:dispatch [:connection/set-window-size {:connr connr
                                                      :width width
                                                      :height height}]])]})))

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
