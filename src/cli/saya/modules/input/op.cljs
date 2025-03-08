(ns saya.modules.input.op
  (:require
   [saya.modules.input.helpers :refer [adjust-scroll-to-cursor clamp-cursor
                                       clamp-scroll]]
   [saya.modules.input.normal :as normal]
   [saya.modules.input.shared :refer [to-end-of-line to-start-of-line]]))

(defn- extract-flags [f]
  (meta f))

(defn movement->motion [f]
  (fn movement-motion [{:keys [pending-operator] :as context}]
    (let [context' (f context)
          start (get-in context [:buffer :cursor])
          end (get-in context' [:buffer :cursor])
          motion-range (merge
                        (extract-flags f)
                        {:start start
                         :end end
                         ; TODO: There's eg o_v for turning a normally line-wise
                         ; motion into a character-wise one
                         :linewise? (or (::linewise? context)
                                        (not= (:row start) (:row end)))})
          context (dissoc context ::linewise?)]
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

(def keymaps
  (->> normal/movement-keymaps
       (map (fn [[k v]]
              [k (movement->motion v)]))
       (into {})))

(def full-line-keymap
  {[:full-line] (comp
                 (movement->motion #'to-end-of-line)
                 (fn [ctx]
                   (assoc ctx ::linewise? true))
                 to-start-of-line)})
