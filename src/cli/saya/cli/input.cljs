(ns saya.cli.input
  (:require [archetype.util :refer [>evt]]
            ["ink" :as k]
            [saya.cli.keys :refer [->key]]
            [saya.modules.input.core :as input]))

(defn dispatcher []
  (k/useInput
   (fn input-dispatcher [input k]
     (let [the-key (->key input k)]
       (>evt [::input/on-key the-key]))))

  ; This is a functional component that doesn't render anything
  nil)
