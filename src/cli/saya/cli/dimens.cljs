(ns saya.cli.dimens
  (:require
   ["ink" :as k]
   ["react" :as React]
   [applied-science.js-interop :as j]
   [archetype.util :refer [>evt]]
   [saya.events :as events]))

(defn use-stdout-dimensions []
  (j/let [^js {:keys [stdout]} (k/useStdout)
          [dimens set-dimens!] (React/useState
                                [(.-columns stdout)
                                 (.-rows stdout)])]

    (React/useEffect
     (fn []
       (let [handle-resize (fn handle-resize []
                             (set-dimens!
                              [(.-columns stdout)
                               (.-rows stdout)]))]
         (.on stdout "resize" handle-resize)

         (fn on-unmount []
           (.off stdout "resize" handle-resize))))

     #js [stdout])

    dimens))

(defn dimens-tracker []
  (let [[width height] (use-stdout-dimensions)]
    (>evt [::events/set-dimens width height])))
