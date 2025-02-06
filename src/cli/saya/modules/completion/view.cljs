(ns saya.modules.completion.view
  (:require
   [archetype.util :refer [<sub]]
   [saya.modules.completion.subs :as subs]
   [saya.modules.popup.view :refer [popup-menu pum-line]]))

(defn completion-line [{:keys [selected?]} & text]
  ; TODO: Theming, I suppose?
  (into [pum-line {:background-color :gray
                   :inverse selected?}]
        text))

(defn completion-menu []
  (let [left-offset (<sub [::subs/left-offset])
        applied-candidate (<sub [::subs/applied-candidate])
        candidates (<sub [::subs/candidates])]
    (when left-offset
      [popup-menu {:flex-direction :column
                   :left left-offset}
       (for [c candidates]
         ^{:key c}
         [completion-line {:selected? (= c applied-candidate)} c])])))
