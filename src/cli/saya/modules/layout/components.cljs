(ns saya.modules.layout.components
  (:require
   ["ink" :as k]
   [clojure.string :as str]))

(defn- build-box [{:keys [background-color flex-direction height width]}]
  [:> k/Box {:flex-direction flex-direction
             :flex-grow (when-not (number? height)
                          1)
             :background-color (when (or (string? background-color)
                                         (keyword? background-color))
                                 background-color)
             :height (when (number? height)
                       height)
             :width (if (or (number? width)
                            (keyword? width))
                      width
                      :100%)
             :flex 1}])

(defn horizontal [opts & children]
  (into (build-box (assoc opts :flex-direction :row))
        children))

(defn vertical [opts & children]
  (into (build-box (assoc opts :flex-direction :column))
        children))

(defn edit-file-view [filename]
  [:> k/Box {:flex-direction :row
             :flex-grow 1
             :width :100%
             :flex 1}
   ; TODO:
   [:> k/Text "TODO: file@" (str filename)]])

(defn edit-string-view [content]
  [:> k/Box {:flex-direction :row
             :flex-grow 1
             :width :100%
             :flex 1}
   [:> k/Text "TODO: " (str content)]])

(defn edit-ref-view [the-ref]
  (let [v (try @the-ref
               (catch :default e
                 (str "ERROR: Unable to deref reference: " e)))]
    [edit-string-view (if (string? v)
                        v
                        (str/join "\n" v))]))
