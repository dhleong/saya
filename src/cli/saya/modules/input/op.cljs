(ns saya.modules.input.op
  (:require
   [saya.modules.input.helpers :refer [adjust-scroll-to-cursor clamp-cursor
                                       clamp-scroll]]
   [saya.modules.input.normal :as normal]))

(defn movement->motion [f]
  (fn movement-motion [{:keys [pending-operator] :as context}]
    (let [context' (f context)
          start (get-in context [:buffer :cursor])
          end (get-in context' [:buffer :cursor])
          motion-range {:start start :end end}]
      (if-not (= start end)
        (-> context
            (pending-operator motion-range)
            (as-> context''
              ; In case the operator returned an error, for example:
              (merge context context''))
            (clamp-cursor)
            (adjust-scroll-to-cursor)
            (clamp-scroll))

        context))))

; TODO: Perform motions from normal mode
(def keymaps
  (->> normal/movement-keymaps
       (map (fn [[k v]]
              [k (movement->motion v)]))
       (into {})))

