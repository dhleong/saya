(ns saya.modules.command.events
  (:require
   [re-frame.core :refer [reg-event-db trim-v]]
   [saya.modules.buffers.line :refer [buffer-line]]))

(reg-event-db
 ::prepare-buffer
 [trim-v]
 (fn [db [pending-input-line]]
   (assoc-in
    db
    [:buffers :cmd]
    {:lines (-> (get-in db [:histories :cmd])
                (->> (map buffer-line))
                (reverse)
                (vec)
                (conj (buffer-line pending-input-line)))})))
