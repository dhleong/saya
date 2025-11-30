(ns saya.config)

(def debug?
  ^boolean goog.DEBUG)

(goog-define testing? false)

(def echo-prompt-window-ms 250)

; Potentially useful for debugging rendering output
(def no-ansi? false)

