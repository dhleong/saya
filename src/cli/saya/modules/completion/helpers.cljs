(ns saya.modules.completion.helpers
  (:require
   [archetype.util :refer [>evt]]
   [clojure.string :as str]
   [promesa.core :as p]
   [saya.modules.completion.events :as events]
   [saya.modules.completion.proto :as proto :refer [ICompletionSource]]
   [saya.modules.logging.core :refer [log]]))

(defn word-to-complete [{:keys [line-before-cursor]}]
  (if line-before-cursor
    (if-some [last-whitespace-idx (str/last-index-of line-before-cursor " ")]
      (subs line-before-cursor (inc last-whitespace-idx))
      line-before-cursor)
    ""))

(defn refresh-completion [^ICompletionSource source, bufnr line cursor]
  (let [line-before-cursor (subs line 0 cursor)
        context {:line-before-cursor line-before-cursor}
        word-to-complete (word-to-complete context)]
    (>evt [::events/start {:bufnr bufnr
                           :word-to-complete word-to-complete
                           :line-before-cursor line-before-cursor}])

    (when (seq word-to-complete)
      (-> (p/let [candidates (proto/gather-candidates source context)]
            (>evt [::events/on-candidates {:bufnr bufnr
                                           :candidates (seq candidates)}]))
          (p/catch (fn [e]
                     (log "ERROR in completion: " e)
                     (>evt [::events/on-error {:bufnr bufnr
                                               :error e}])))))))
