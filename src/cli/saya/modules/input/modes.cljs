(ns saya.modules.input.modes
  (:require
   [clojure.set :refer [map-invert]]))

(def mode->bufnr
  {:command :cmd
   :search :search})

(def bufnr->mode (map-invert mode->bufnr))

(defn command-like? [mode]
  (contains? mode->bufnr mode))
