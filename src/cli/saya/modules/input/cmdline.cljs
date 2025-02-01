(ns saya.modules.input.cmdline
  (:require
   ["react" :as React]
   [reagent.core :as r]))

(defonce ^:private cmdline-node (r/atom nil))
(def set-node! (partial reset! cmdline-node))

(defn <cmdline-window []
  @cmdline-node)

(defn >cmdline-window [& content]
  (React/useEffect
   (fn []
     (set-node! (into [:<>] content))
     (fn unmount-cmdline-window []
       (set-node! nil))))
  nil)
