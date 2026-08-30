(ns saya.views
  (:require
   ["ink" :as k]
   [archetype.util :refer [<sub]]
   [saya.cli.fullscreen :refer [dimens-tracker fullscreen-box]]
   [saya.cli.input :as input]
   [saya.modules.completion.view :refer [completion-menu]]
   [saya.modules.home.core :refer [home-view]]
   [saya.modules.ui.error-boundary :refer [error-boundary]]))

(def ^:private pages
  {:home #'home-view})

(defn- popup-menus []
  [:<>
   [completion-menu]])

(defn main []
  (let [[page args] (<sub [:page])
        page-fn (get pages page)
        page-form [:f> page-fn args]]
    [:<>
     [:f> dimens-tracker]
     [:f> input/dispatcher]

     [fullscreen-box {:flex-direction :column}
      [error-boundary
       (cond
         (not page-fn)
         [:> k/Text
          [:> k/Text {:background-color "red"} " ERROR "]
          " No page registered for: "
          [:> k/Text {:color "gray"} ; TODO: Theming?
           (str [page args])]]

         :else
         page-form)]]

     ; Popup menus need to live here for absolute positioning to work correctly:
     [error-boundary
      [popup-menus]]]))
