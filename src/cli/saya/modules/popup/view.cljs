(ns saya.modules.popup.view
  (:require
   ["ink" :as k]
   ["react" :as React]
   ["string-width" :default string-width]
   [applied-science.js-interop :as j]
   [archetype.util :refer [<sub]]
   [clojure.string :as str]))

(defonce ^:private popup-menu-context (React/createContext nil))

(def ^:private clear-style "\u001b[49m")

(defn pum-line
  "Almost-drop-in replacement for k/Text that ensures the line fills the full
   width of the popup. This is almost certainly a terrible idea and you should
   probably just fill in space yourself"
  [options & children]
  (let [{:keys [width]} (React/useContext popup-menu-context)]
    [:> k/Transform {:transform (fn [text]
                                  (let [plain-length (string-width text)
                                        text (cond-> text
                                               (str/ends-with? text clear-style)
                                               (subs 0 (- (count text)
                                                          (count clear-style))))]
                                    (apply str text (concat (repeat (- width plain-length) " ")
                                                            [clear-style]))))}
     (into [:> k/Text options] children)]))

(defn popup-menu [options & children]
  (let [box-ref (React/useRef)
        [box-dimens set-box-dimens!] (React/useState)

        {:keys [x y]} (<sub [:global-cursor])
        {:keys [height]} (<sub [:dimens])
        below? (<= y 5)
        positioning (if below?
                      {:top y}
                      {:bottom (- height y)})]

    (React/useLayoutEffect
     (fn []
       (when-some [el box-ref.current]
         (j/let [^:js {:keys [width height]} (k/measureElement el)]
           (set-box-dimens!
            (fn [old-value]
              (if (and (= (:width old-value) width)
                       (= (:height old-value) height))
                  ; Return exact same value to avoid re-render
                old-value

                {:width width :height height})))))

       js/undefined))

    (when x
      [:r> popup-menu-context.Provider #js {:value box-dimens}
       (into [:> k/Box (merge
                        options
                        positioning
                        {:ref box-ref
                         :position :absolute
                         :left (+ x (:left options 0))})]
             children)])))
