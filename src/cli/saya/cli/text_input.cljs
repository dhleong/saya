(ns saya.cli.text-input
  (:require
   ["ink-text-input" :default TextInput]))

(defn text-input [{:keys [value on-change on-submit]}]
  ; TODO: Eventually we may want our own input component. This one is a little janky
  [:> TextInput {:value value
                 :on-change on-change
                 :show-cursor false
                 :on-submit on-submit}])
