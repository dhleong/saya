(ns saya.util.paths
  (:require [applied-science.js-interop :as j]
            ["env-paths" :as env-paths]
            ["node:os" :as os]
            ["node:path" :as path]))

(def ^:private app-name "saya")

(defn platform [kind]
  (-> (env-paths app-name, #js {:suffix ""})
      (j/get kind)))

(defn user-config
  ; TODO: Respect XDG_CONFIG?
  ([] (path/join
       (os/homedir)
       ".config"
       app-name))
  ([sub-path]
   (path/join (user-config) sub-path)))

(defn internal-data
  ([] (platform :data))
  ([sub-path]
   (path/join (internal-data) sub-path)))
