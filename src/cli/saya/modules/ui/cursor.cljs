(ns saya.modules.ui.cursor
  (:require
   ["ink" :as k]
   ["react" :as React]
   [applied-science.js-interop :as j]))

(defonce ^:private element-ref (atom nil))
(defonce ^:private shape-ref (atom :block))

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

(defn get-cursor-shape []
  (or @shape-ref :block))

(defn- f>cursor [shape]
  (let [ref (React/useRef)]
    (React/useEffect
     (fn []
       (reset! element-ref (j/get ref :current))
       (reset! shape-ref shape)

       (fn unmount []
         (reset! element-ref nil))))
    [:> k/Box {:ref ref}]))

(defn cursor
  ([] [cursor :block])
  ([shape] [:f> f>cursor shape]))
