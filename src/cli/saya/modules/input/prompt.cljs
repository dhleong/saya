(ns saya.modules.input.prompt
  (:require
   [saya.modules.input.helpers :refer [update-cursor]]
   [saya.modules.input.normal :as normal :refer [mode<-]]))

(defn scroll-input-history [direction {:keys [buffer] :as ctx}]
  ctx)

(def prompt-keymaps
  {["k"] (partial scroll-input-history :older)
   ["j"] (partial scroll-input-history :newer)})

(def mode-change-keymaps
  {[:ctrl/k] (comp
              (update-cursor :row dec)
              (mode<- :normal))
   [:ctrl/j] (comp
              (update-cursor :row inc)
              (mode<- :normal))})

(def keymaps
  (merge
   normal/keymaps
   prompt-keymaps
   mode-change-keymaps))
