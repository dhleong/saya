(ns saya.modules.input.history.search
  (:require
   ["fzf" :refer [Fzf]]
   [applied-science.js-interop :as j]
   [promesa.core :as p]
   [re-frame.db :as rfdb]
   [saya.modules.completion.proto :refer [ICompletionSource
                                          IConditionalCompletionSource]]))

(defn- perform-query [bufnr request]
  (let [db @rfdb/app-db
        history (get-in db [:histories bufnr])]
    (if (seq request)
      (-> ^js (Fzf. (to-array history))
          (.find request)
          (->> (map #(j/get % .-item))))
      history)))

(defrecord HistoryCompletionSource [bufnr]
  IConditionalCompletionSource
  (should-gather? [_this _ctx]
    true)

  ICompletionSource
  (gather-candidates [_this {:keys [line-before-cursor]}]
    (p/do
      (perform-query bufnr line-before-cursor))))

(comment
  (perform-query [:conn/input 0] "he"))
