(ns saya.modules.command.registry
  (:require
   [re-frame.registrar :refer [get-handler]]
   [saya.modules.command.core]))

(defn- extract-aliases [registered-handler]
  ; registered-handler looks like '({:id ...} ...)
  (when-let [aliases-interceptor
             (->> registered-handler
                  (filter #(= :command/aliases (:id %)))
                  first)]
    (:comment aliases-interceptor)))

(defn- resolve-registered-commands []
  ; NOTE: The naked requires above are important for
  ; making this work
  (->> (get-handler :event)
       (into {}
             (filter (fn [[k _]]
                       (= "command" (namespace k)))))))

(def registered-commands
  (delay
    (resolve-registered-commands)))

(defn- resolve-commands-by-aliases []
  (->> @registered-commands
       (into {}
             (mapcat (fn [[k v]]
                       (concat
                        [[(name k) k]]

                        (map
                         (fn [alias]
                           [(name alias) k])
                         (extract-aliases v))))))))

(def commands-by-aliases
  (delay
    (resolve-commands-by-aliases)))
