(ns saya.modules.logging.core
  (:require
   [archetype.util :refer [>evt]]
   [clojure.string :as str]
   [saya.modules.logging.events :as events]))

(defn log [& vals]
  (let [msg (str/join " " vals)]
    (>evt [::events/log (js/Date.now) msg])))
