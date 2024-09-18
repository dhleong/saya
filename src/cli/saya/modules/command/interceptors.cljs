(ns saya.modules.command.interceptors
  (:require
   [re-frame.core :refer [->interceptor]]))

(defn aliases [& command-aliases]
  (->interceptor
   {:id :command/aliases
    :comment (vec command-aliases)}))
