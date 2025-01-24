(ns saya.modules.input.normal)

(def movement-keymaps
  {["0"] (fn to-start-of-line [{:keys [buffer]}]
           {:buffer (assoc-in buffer [:cursor :col] 0)})

   ["g" "g"] (fn to-first-line [{:keys [buffer]}]
               ; TODO: Move [:window :anchor-row]
               {:buffer (assoc-in buffer [:cursor] {:col 0
                                                    :row 0})})

   ["G"] (fn to-last-line [{:keys [buffer]}]
           {:buffer (assoc-in buffer [:cursor :row]
                              (dec (count (:lines buffer))))})

   ; Single char movement
   ["k"] (fn [{:keys [buffer]}]
           {:buffer (update-in buffer [:cursor :row]
                               (comp (partial max 0) dec))})
   ["h"] (fn [{:keys [buffer]}]
           {:buffer (update-in buffer [:cursor :col]
                               (comp (partial max 0) dec))})})

(def keymaps
  (merge
   movement-keymaps))
