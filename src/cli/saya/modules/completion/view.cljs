(ns saya.modules.completion.view
  (:require
   [saya.modules.popup.view :refer [popup-menu pum-line]]))

(defn completion-line [& text]
  (into [pum-line {:background-color :red}]
        text))

(defn completion-menu []
  [popup-menu {:flex-direction :column}
   [completion-line "Hi!"]
   [completion-line "There"]])
