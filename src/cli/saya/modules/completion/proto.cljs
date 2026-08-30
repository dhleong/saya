(ns saya.modules.completion.proto)

(defprotocol ICompletionSource
  (gather-candidates [this {:keys [line-before-cursor]}]))

(defprotocol IConditionalCompletionSource
  (should-gather? [this {:keys [line-before-cursor]}]))
