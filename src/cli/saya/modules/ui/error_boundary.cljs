(ns saya.modules.ui.error-boundary
  (:require
   ["ink" :as k]
   [reagent.core :as r]))

(when goog.DEBUG
  ; in debug builds, we can auto-retry rendering error'd components
  ; every time a load occurs
  (def ^:private active-err-atoms (atom #{}))

  #_{:clj-kondo/ignore [:unused-private-var]}
  (defn- ^:dev/after-load clear-errors []
    (swap! active-err-atoms (fn [atoms]
                              (doseq [a atoms]
                                (reset! a nil))

                              ; clear
                              #{}))))

(defn- error-view [props _err-atom info-atom error]
  (let [{:keys [clean-error clean-component-stack]
         :or {clean-component-stack identity}} props]
    [:> k/Box {:flex-direction :column}
     [:> k/Text {:color :red
                 :inverse true}
      "Oops! Something went wrong"]

     (when-let [^js info @info-atom]
       [:<>
        [:> k/Text "Component Stack:"]
        [:> k/Box {:overflowY :hidden}
         [:> k/Text (clean-component-stack
                     (.-componentStack info))]]])

     [:> k/Newline]

     [:> k/Text {:color :red
                 :inverse true}
      "Error:"]
     [:> k/Box {:overflowY :hidden}
      [:> k/Text
       (cond
         clean-error (clean-error error)
         (ex-message error) (.-stack error)
         :else (str error))]]]))

(defn error-boundary [& _]
  (r/with-let [err (r/atom nil)
               info-atom (r/atom nil)]
    (r/create-class
     {:display-name "Error Boundary"

      :component-did-catch (fn [_this error info]
                             (js/console.warn error info)
                             (reset! err error)
                             (reset! info-atom info))

      :statics
      #js {:getDerivedStateFromError (fn [error]
                                       (when goog.DEBUG
                                          ; enqueue the atom for auto-clearing
                                         (swap! active-err-atoms conj err))

                                       (reset! err error))}

      :reagent-render (fn [& children]
                        (let [props (when (map? (first children))
                                      (first children))
                              children (if (map? (first children))
                                         (rest children)
                                         children)]
                          (if-let [e @err]
                            [error-view props err info-atom e]

                            (into [:<>] children))))})))
