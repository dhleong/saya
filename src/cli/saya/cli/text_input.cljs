(ns saya.cli.text-input
  (:require
   ["ink" :as k]
   ["ink-text-input" :default TextInput]
   [saya.modules.ui.cursor :refer [cursor]]))

(defn text-input [{:keys [value on-change on-submit]}]
  ; TODO: Eventually we may want our own input component. This one is a little janky
  [:> k/Text
   [:> TextInput {:value value
                  :on-change on-change
                  :show-cursor false
                  :on-submit on-submit}]
   [cursor]])
