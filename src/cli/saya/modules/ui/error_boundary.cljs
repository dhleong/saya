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

(defn- title [style text]
  [:> k/Box {:min-height 1}
   [:> k/Text style
    text]])

(defn- section-title [text]
  [title {:color :red
          :inverse true}
   text])

(defn- error-view [props _err-atom info-atom error]
  (let [{:keys [clean-error clean-component-stack]
         :or {clean-component-stack identity}} props]
    [:> k/Box {:flex-direction :column
               :justify-content :center}
     [section-title "Oops! Something went wrong"]

     (when-let [^js info @info-atom]
       [:<>
        [title {:bold true} "Component Stack:"]
        [:> k/Box {:overflow :hidden
                   :flex-basis 50}
         [:> k/Text
          (clean-component-stack
           (.-componentStack info))]]])

     [:> k/Newline]

     [section-title "Error:"]
     [:> k/Box {:overflow :hidden
                :flex-basis 50}
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
                             (reset! err error)
                             (reset! info-atom info))

      :get-derived-state-from-error (fn [error]
                                      (when goog.DEBUG
                                    ; enqueue the atom for auto-clearing
                                        (swap! active-err-atoms conj err))

                                      (reset! err error))

      :reagent-render (fn [& children]
                        (let [props (when (map? (first children))
                                      (first children))
                              children (if (map? (first children))
                                         (rest children)
                                         children)]
                          (if-let [e @err]
                            [error-view props err info-atom e]

                            (into [:<>] children))))})))
