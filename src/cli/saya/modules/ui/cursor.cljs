(ns saya.modules.ui.cursor
  (:require
   ["ink" :as k]
   ["react" :as React]
   [applied-science.js-interop :as j]))

(defonce ^:private element-ref (atom nil))

(defn- compute-absolute-pos [^js element]
  (loop [node (.-yogaNode element)
         x (.getComputedLeft node)
         y (.getComputedTop node)]
    (if-let [parent (.getParent node)]
      (recur parent
             (+ x (.getComputedLeft parent))
             (+ y (.getComputedTop parent)))

      {:x x :y y})))

(defn get-cursor-position []
  (when-some [element @element-ref]
    (compute-absolute-pos element)))

(defn- f>cursor []
  (let [ref (React/useRef)]
    (React/useEffect
     (fn []
       (reset! element-ref (j/get ref :current))

       (fn unmount []
         (reset! element-ref nil))))
    [:> k/Box {:ref ref}]))

(defn cursor []
  [:f> f>cursor])
