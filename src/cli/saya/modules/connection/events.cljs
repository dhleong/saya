(ns saya.modules.connection.events
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.input.events :as input-events]
   [saya.modules.window.events :as window-events]))

(reg-event-fx
 ::submit-input-buffer
 [unwrap]
 (fn [{:keys [db]} {:keys [connr text]}]
   ; NOTE: We support providing the text explicitly for cases like submitting
   ; from cmdline window, where reading from the buffer here may not work.
   ; There's probably some cleanup worth investigating there, but... this is
   ; fine for now.
   (let [bufnr [:conn/input connr]
         text (or text
                  (->> (get-in db [:buffers bufnr :lines])
                       (str/join "\n")))]
     (when bufnr
       {:fx [[:dispatch
              ; NOTE: Ensure input is cleared; on-persist-value *may not*
              ; be called from the cmdline window. This is kinda hacks,
              ; but fixing properly in input-window feels... annoying
              [::window-events/set-input-text {:connr connr
                                               :text ""}]]
             [:dispatch
              [::input-events/add-history {:bufnr bufnr
                                           :entry text}]]
             [:dispatch
              [::buffer-events/set-cursor
               {:id bufnr
                :cursor {:row 0 :col 0}}]]
             [:dispatch
              [:connection/send {:connr connr
                                 :text text}]]]}))))
