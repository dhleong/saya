(ns saya.util.paths
  (:require [applied-science.js-interop :as j]
            ["env-paths" :as env-paths]
            ["node:os" :as os]
            ["node:path" :as path]
            ["untildify" :default untildify]))

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

(defn resolve-user
  "Given a user-provided path, resolve the absolute path to it,
   handling things like ~ for HOME"
  [user-path]
  (-> (untildify user-path)
      (path/resolve)))
