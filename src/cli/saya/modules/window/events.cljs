(ns saya.modules.window.events
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-db reg-event-fx unwrap]]
   [saya.modules.buffers.line :refer [buffer-line]]
   [saya.modules.input.helpers :refer [adjust-scroll-to-cursor]]
   [saya.modules.input.keymaps :refer [build-context]]))

(reg-event-fx
 ::on-measured
 [unwrap]
 (fn [{:keys [db] :as cofx} {:keys [id width height]}]
   (let [{:keys [bufnr]} (get-in db [:windows id])
         connr (get-in db [:buffers bufnr :connection-id])
         ctx' (-> cofx
                  (assoc :bufnr bufnr :winnr id)
                  (build-context)
                  (update :window merge {:width width
                                         :height height})
                  (adjust-scroll-to-cursor))]
     {:db (assoc-in db [:windows id] (:window ctx'))
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
                  (mapv buffer-line)))))

(reg-event-db
 ::prepare-input-cmdline-buffer
 [unwrap]
 (fn [db {:keys [bufnr current]}]
   (let [history (get-in db [:histories bufnr])]
     (assoc-in db [:buffers bufnr :lines]
               (-> history
                   (->> (map buffer-line))
                   (vec)
                   (conj (buffer-line current)))))))
