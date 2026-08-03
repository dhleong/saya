(ns saya.cli.fullscreen
  (:require
   ["ink" :as k]
   [applied-science.js-interop :as j]
   [archetype.util :refer [<sub >evt]]
   [saya.events :as events]))

(defn dimens-tracker []
  (j/let [^js {:keys [columns rows]} (k/useWindowSize)]
    (>evt [::events/set-dimens columns rows])))

(defn fullscreen-box [& children]
  (let [[props children] (if (map? (first children))
                           [(first children)
                            (rest children)]
                           [nil
                            children])
        dimens (<sub [:dimens])
        props (merge dimens props)]
    (into [:> k/Box props] children)))
