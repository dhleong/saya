(ns saya.modules.input.layout
  (:require
   [saya.modules.layout.core :as layout]))

(defn- move-cursor [navigate-fn]
  (fn move-cursor-fn [{:keys [layout window buffer
                              :layout/lookup-keys
                              :layout/keys]}]
    (def last-state [(:connection-id buffer) layout lookup-keys])
    (let [evaluated (layout/evaluate layout)
          src-key (or (get-in lookup-keys [:winnr (:id window)])
                      (when (:connection-id buffer)
                        (when-some [script-file (:script-file layout)]
                          (get-in lookup-keys [:script-file/connection script-file]))))
          z (layout/zipper-at-key
             evaluated
             src-key)]
      (when-some [z' (navigate-fn z)]
        (let [dest-key (layout/zipper-key z')]
          {:current-winnr (get-in keys [dest-key :winnr])})))))

(def layout-keymaps
  {[:ctrl/w :ctrl/h] (move-cursor #'layout/navigate-left)
   [:ctrl/w "h"] (move-cursor #'layout/navigate-left)

   [:ctrl/w :ctrl/l] (move-cursor #'layout/navigate-right)
   [:ctrl/w "l"] (move-cursor #'layout/navigate-right)

   [:ctrl/w :ctrl/k] (move-cursor #'layout/navigate-up)
   [:ctrl/w "k"] (move-cursor #'layout/navigate-up)

   [:ctrl/w :ctrl/j] (move-cursor #'layout/navigate-down)
   [:ctrl/w "j"] (move-cursor #'layout/navigate-down)})
