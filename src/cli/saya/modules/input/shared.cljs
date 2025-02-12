(ns saya.modules.input.shared
  (:require
   [saya.modules.input.helpers :refer [current-buffer-line-last-col]]))

(defn to-start-of-line [{:keys [buffer]}]
  {:buffer (assoc-in buffer [:cursor :col] 0)})

(defn to-end-of-line [{:keys [buffer]}]
  {:buffer (assoc-in buffer [:cursor :col]
                     (current-buffer-line-last-col buffer))})

