(ns saya.modules.input.shared
  (:require
   [saya.modules.input.helpers :refer [*mode* current-buffer-line-last-col]]))

(defn to-start-of-line [ctx]
  (assoc-in ctx [:buffer :cursor :col] 0))

(defn ^:inclusive? to-end-of-line [{:keys [buffer] :as ctx}]
  (binding [*mode* (:mode ctx *mode*)]
    (assoc-in ctx [:buffer :cursor :col]
              (current-buffer-line-last-col buffer))))

