(ns saya.modules.input.normal
  (:require
   [saya.modules.input.helpers :refer [adjust-scroll clamp-cursor]]))

(defn- update-cursor [col-or-row f]
  (comp
   adjust-scroll
   clamp-cursor
   (fn cursor-updator [ctx]
     (update-in ctx [:buffer :cursor col-or-row] f))))

(def movement-keymaps
  {["0"] (fn to-start-of-line [{:keys [buffer]}]
           {:buffer (assoc-in buffer [:cursor :col] 0)})

   ["g" "g"] (comp
              adjust-scroll
              (fn to-first-line [{:keys [buffer]}]
                {:buffer (assoc-in buffer [:cursor] {:col 0
                                                     :row 0})}))

   ["G"] (comp
          adjust-scroll
          (fn to-last-line [{:keys [buffer] :as ctx}]
            (assoc-in ctx [:buffer :cursor :row]
                      (dec (count (:lines buffer))))))

   ; Single char movement
   ["k"] (update-cursor :row dec)
   ["j"] (update-cursor :row inc)
   ["h"] (update-cursor :col dec)
   ["l"] (update-cursor :col inc)})

(def scroll-keymaps
  {[:ctrl/b] (comp
              clamp-cursor
              adjust-scroll
              (fn scroll-back-page [{:keys [buffer window] :as ctx}]
                (update-in ctx [:window :anchor-row]
                           (fnil - (dec (count (:lines buffer))))
                           (max 0 (dec (:height window))))))})

(def keymaps
  (merge
   movement-keymaps
   scroll-keymaps))
