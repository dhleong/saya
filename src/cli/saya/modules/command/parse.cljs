(ns saya.modules.command.parse
  (:require
   [saya.modules.command.registry :refer [commands-by-aliases]]))

(defn parse-command [s]
  ; TODO: Parse args, etc.
  (let [command s]
    {:command s
     :event (get @commands-by-aliases command)}))
