(ns saya.modules.completion.proto)

(defprotocol ICompletionSource
  (gather-candidates [this {:keys [line-before-cursor]}]))
