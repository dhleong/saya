(ns saya.modules.layout.fx
  (:require
   [re-frame.core :refer [reg-fx]]))

(reg-fx
 ::unsubscribe-from-layout-atom
 (fn [state-atom]
   (remove-watch state-atom :saya/watch)))

(reg-fx
 ::subscribe-to-layout-atom
 (fn [{:keys [_layout-id state-atom]}]
   (add-watch
    state-atom
    :saya/watch
    (fn [_key _ref _old-state _new-state]
      ; TODO: Trigger a re-render
      ))))
