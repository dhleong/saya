(ns saya.modules.connection.completion
  (:require
   [promesa.core :as p]
   [saya.modules.completion.proto :refer [ICompletionSource]]
   [saya.modules.kodachi.api :as api]))

(defrecord ConnectionCompletionSource [connr]
  ICompletionSource
  (gather-candidates [_this {:keys [line-before-cursor]}]
    (p/let [response (api/request! {:type :CompleteComposer
                                    :connection_id connr
                                    :line_to_cursor line-before-cursor})]
      (:words response))))
