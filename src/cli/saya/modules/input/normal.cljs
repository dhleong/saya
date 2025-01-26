(ns saya.modules.input.normal
  (:require
   [saya.modules.input.helpers :refer [adjust-cursor-to-scroll
                                       adjust-scroll-to-cursor clamp-cursor
                                       clamp-scroll
                                       current-buffer-line-last-col
                                       last-buffer-row]]))

(defn- update-cursor [col-or-row f]
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn cursor-updator [ctx]
     (update-in ctx [:buffer :cursor col-or-row] f))))

(def scroll-to-bottom
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn to-last-line [{:keys [buffer] :as ctx}]
     (assoc-in ctx [:buffer :cursor :row]
               (last-buffer-row buffer)))))

(def movement-keymaps
  {["0"] (fn to-start-of-line [{:keys [buffer]}]
           {:buffer (assoc-in buffer [:cursor :col] 0)})
   ["$"] (fn to-end-of-line [{:keys [buffer]}]
           {:buffer (assoc-in buffer [:cursor :col]
                              (current-buffer-line-last-col buffer))})

   ["g" "g"] (comp
              clamp-scroll
              adjust-scroll-to-cursor
              (fn to-first-line [ctx]
                (assoc-in ctx [:buffer :cursor] {:col 0
                                                 :row 0})))

   ["G"] scroll-to-bottom

   ; Single char movement
   ["k"] (update-cursor :row dec)
   ["j"] (update-cursor :row inc)
   ["h"] (update-cursor :col dec)
   ["l"] (update-cursor :col inc)})

(defn- update-scroll [f compute-amount]
  (comp
   adjust-scroll-to-cursor
   adjust-cursor-to-scroll
   clamp-cursor
   clamp-scroll
   (fn scroll-updater [{:keys [buffer] :as ctx}]
     (update-in ctx [:window :anchor-row]
                (fnil f (last-buffer-row buffer))
                (max 0 (compute-amount ctx))))))

(defn- window-rows [{:keys [window]}]
  (dec (:height window)))

(def scroll-keymaps
  {[:ctrl/y] (update-scroll - (constantly 1))
   [:ctrl/e] (update-scroll + (constantly 1))

   [:ctrl/b] (update-scroll - window-rows)
   [:ctrl/f] (update-scroll + window-rows)})

(def keymaps
  (merge
   movement-keymaps
   scroll-keymaps))
