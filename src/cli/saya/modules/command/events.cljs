(ns saya.modules.command.events
  (:require
   [re-frame.core :refer [reg-event-db trim-v]]
   [saya.modules.buffers.line :refer [buffer-line]]))

(reg-event-db
 ::prepare-buffer
 [trim-v]
 (fn [db [pending-input-line]]
    ; TODO: cmdline history
   (assoc-in db [:buffers :cmd] {:lines [(buffer-line pending-input-line)]})))
