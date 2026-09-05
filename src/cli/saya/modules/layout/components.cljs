(ns saya.modules.layout.components
  (:require
   ["ink" :as k]
   [clojure.string :as str]))

(defn horizontal [_opts & children]
  (into [:> k/Box {:flex-direction :row
                   :flex-grow 1
                   :width :100%
                   :flex 1}]
        children))

(defn vertical [_opts & children]
  (into [:> k/Box {:flex-direction :column
                   :flex-grow 1
                   :width :100%
                   :flex 1}]
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
