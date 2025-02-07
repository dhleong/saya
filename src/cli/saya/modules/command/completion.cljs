(ns saya.modules.command.completion
  (:require
   [clojure.string :as str]
   [promesa.core :as p]
   [saya.modules.command.registry :refer [registered-commands]]
   [saya.modules.completion.helpers :as helpers]
   [saya.modules.completion.proto :refer [ICompletionSource]]))

(defrecord CommandCompletionSource []
  ICompletionSource
  (gather-candidates [_this context]
    (let [word (helpers/word-to-complete context)]
      (p/do
        (->> @registered-commands
             keys
             (map name)
             ; TODO: Possibly, fuzzy:
             (filter #(str/starts-with? % word)))))))
