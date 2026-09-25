(ns saya.util.hooks
  (:require-macros
   [saya.util.hooks])
  (:require
   ["react" :as React]
   [applied-science.js-interop :as j]))

(defn use-effect [f deps]
  (React/useEffect
   (fn []
     (let [resp (f)]
       (if (fn? resp)
         resp
         js/undefined)))
   deps))
(defn use-mount-effect [f]
  (let [r (React/useRef f)]
    (use-effect
     (fn [] (j/call-in r [.-current]))
     #js [])))

