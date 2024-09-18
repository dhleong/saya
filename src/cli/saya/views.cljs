(ns saya.views
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.cli.dimens :refer [dimens-tracker]]
   [saya.views.home.core :refer [home-view]]))

(def ^:private pages
  {:home #'home-view})

(defn main []
  (let [[page args] (<sub [:page])
        page-fn (get pages page)
        page-form [:f> page-fn args]]
    [:<>
     [:f> dimens-tracker]

     (cond
       (not page-fn)
       [:> k/Text
        [:> k/Text {:background-color "red"} " ERROR "]
        " No page registered for: "
        [:> k/Text {:color "gray"} ; TODO: Theming?
         (str [page args])]]

       :else
       page-form)]))
