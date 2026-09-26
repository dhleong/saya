(ns saya.modules.input.layout
  (:require
   [clojure.core.match :as m]
   [saya.modules.layout.core :as layout]
   [saya.modules.layout.subs :refer [connection-window-for-script-file]]))

(defn- move-cursor [navigate-fn]
  (fn move-cursor-fn [{:keys [layout window buffer
                              :readonly/db
                              :layout/lookup-keys
                              :layout/keys]}]
    ; NOTE: I don't love how magical the :connection
    ; wiring is here...
    (let [evaluated (layout/evaluate layout)
          src-key (or (get-in lookup-keys [:winnr (:id window)])
                      (when (:connection-id buffer)
                        (when-some [script-file (:script-file layout)]
                          (get-in lookup-keys [:script-file/connection script-file]))))
          z (layout/zipper-at-key
             evaluated
             src-key)]
      (when-some [z' (navigate-fn z)]
        (let [dest-key (layout/zipper-key z')
              winnr (m/match [(last dest-key)]
                      ; Yuck case:
                      [{:connection script-file}]
                      (connection-window-for-script-file
                       (merge
                        (select-keys db [:windows :connections])
                        {:script-file-data
                         (get-in db [:script-files script-file])}))

                      ; Easy case:
                      :else (get-in keys [dest-key :winnr]))]
          {:current-winnr winnr})))))

(def layout-keymaps
  {[:ctrl/w :ctrl/h] (move-cursor #'layout/navigate-left)
   [:ctrl/w "h"] (move-cursor #'layout/navigate-left)

   [:ctrl/w :ctrl/l] (move-cursor #'layout/navigate-right)
   [:ctrl/w "l"] (move-cursor #'layout/navigate-right)

   [:ctrl/w :ctrl/k] (move-cursor #'layout/navigate-up)
   [:ctrl/w "k"] (move-cursor #'layout/navigate-up)

   [:ctrl/w :ctrl/j] (move-cursor #'layout/navigate-down)
   [:ctrl/w "j"] (move-cursor #'layout/navigate-down)})
