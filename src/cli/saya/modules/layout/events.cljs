(ns saya.modules.layout.events
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-fx unwrap]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.layout.core :as layout]))

(reg-event-fx
 ::set-current-tab-layout
 [unwrap]
 (fn [cofx {:keys [layout script-file]}]
   (let [layout-id 0 ; TODO: multi-tab support
         db-path [:db :layouts layout-id]
         cofx' (update-in cofx db-path
                          assoc
                          :script-file script-file
                          :id layout-id
                          :component layout)]
     (layout/install cofx' (get-in cofx' db-path)))))

(reg-event-fx
 ::set-keyed-buffer-contents
 [unwrap]
 (fn [{:keys [db]} {:keys [key content file-path string]}]
   (when-let [bufnr (get-in db [:layout/keys key :bufnr])]
     (let [lines (cond
                   (some? string)
                   (str/split-lines string)

                   (string? content)
                   (str/split-lines content)

                   (vector? content)
                   content

                   (sequential? content)
                   (vec content))]
       {:dispatch [::buffer-events/set-string-lines
                   {:id bufnr
                    :file-path file-path
                    :lines lines}]}))))
