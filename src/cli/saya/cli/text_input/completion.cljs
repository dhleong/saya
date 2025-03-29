(ns saya.cli.text-input.completion
  (:require
   [saya.cli.text-input.helpers :refer [split-text-by-state]]
   [saya.modules.completion.helpers :refer [word-to-complete]]))

(defn- try-update-completion [state {:keys [word] :as new-state}]
  ; NOTE: While cycling through candidates, the :word should be
  ; unchanged
  (if (= word (:word state))
    state
    new-state))

(defn- clamp-to-candidates [candidates idx]
  (when-not (or (>= idx (count candidates))
                (< idx 0))
    idx))

(defn next-candidate [state index-pred value completion]
  (let [state' (-> state
                   (update :completion try-update-completion completion))
        candidates (get-in state' [:completion :candidates])]
    (if (seq candidates)
      (let [state' (update-in state' [:completion :index]
                              (comp
                               (partial clamp-to-candidates candidates)
                               index-pred))
            new-candidate (if-some [n (:index (:completion state'))]
                            (nth candidates n)
                            ; Replace with the original input
                            (:word (:completion state')))

            [before after] (split-text-by-state state' value)
            completed-before (word-to-complete {:line-before-cursor before})
            before' (subs before 0 (- (count before)
                                      (count completed-before)))
            result (str before'
                        new-candidate
                        after)]
        (-> state'
            (assoc :cursor (+ (count before') (count new-candidate)))
            (assoc-in [:completion :result] result)))

      state')))

(defn cycle-completion-candidates [{:keys [on-change completion-candidates completion-word]}
                                   state-ref index-pred value]
  (let [new-completion {:word completion-word :candidates completion-candidates}
        [_ new-state] (swap-vals! state-ref next-candidate index-pred value new-completion)]
    ; Storing this result in the state like this is pretty gross:
    (when-let [value' (:result (:completion new-state))]
      (on-change value' (:cursor new-state)
                 {:applied-candidate (when-some [idx (:index (:completion new-state))]
                                       (nth (:candidates (:completion new-state))
                                            idx))}))))
