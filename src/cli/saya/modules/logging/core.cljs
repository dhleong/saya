(ns saya.modules.logging.core
  (:require
   [archetype.util :refer [>evt]]
   [clojure.string :as str]
   [saya.modules.logging.events :as events]))

(defn log-event [& vals]
  (let [msg (str/join " " vals)]
    [::events/log (js/Date.now) msg]))

(defn log-fx [& vals]
  [:dispatch (apply log-event vals)])

(defn log [& vals]
  (>evt (apply log-event vals)))

#_{:clj-kondo/ignore [:unresolved-namespace]}
(comment
  ; Useful for seeing telnet debugging
  (->> (:log @re-frame.db/app-db)
       (filter #(str/starts-with? (:text %) "[kodachi]"))))
