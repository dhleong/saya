(ns saya.modules.ui.cursor
  (:require
   ["ink" :as k]
   ["react" :as React]
   [clojure.string :as str]))

(defonce ^:private shape-ref (atom :block))

(def ^:private cursor-text "\u200B")

(defn extract-cursor-position [lines]
  (loop [y 0
         lines lines]
    (when-some [line (first lines)]
      (if-let [x (str/index-of line cursor-text)]
        ; TODO: if x >= screen width, it should be {:x 0, :y (inc y)}
        {:x x :y y}
        (recur (inc y)
               (next lines))))))

(defn get-cursor-shape []
  (or @shape-ref :block))

(defn- f>cursor [shape]
  (React/useEffect
   (fn []
     (reset! shape-ref shape)

     js/undefined))
  [:> k/Text cursor-text])

(defn cursor
  ([] [cursor :block])
  ([shape] [:f> f>cursor shape]))
