(ns saya.modules.layout.core
  (:require
   [clojure.core.match :as m]
   [saya.modules.layout.components :as components]))

(declare ^:private evaluate-form)

(defn- unpack-layout [component-type args]
  (let [[opts args] (if (map? (first args))
                      [(first args) (next args)]
                      [nil args])]
    (into [component-type opts]
          (map evaluate-form args))))

(defn- evaluate-form [[component & args]]
  (case component
    :horizontal (unpack-layout components/horizontal args)
    :vertical (unpack-layout components/vertical args)
    :edit (m/match [(first args)]
            [{:file path}] [components/edit-file-view path]
            [{:content (s :guard string?)}] [components/edit-string-view s]
            [{:content s}] [components/edit-ref-view s])))

(defn evaluate [{:layout/keys [component state-atom]}]
  (let [rendered (component @state-atom)]
    (evaluate-form rendered)))
