(ns saya.modules.completion.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::current-bufnr
 :-> #(get-in % [:completion :current-bufnr]))

(reg-sub
 ::current-buffer
 :<- [:buffers]
 :<- [::current-bufnr]
 (fn [[buffers bufnr]]
   (get buffers bufnr)))

(reg-sub
 ::state
 :<- [::current-buffer]
 :-> :completion)

(reg-sub
 ::left-offset
 :<- [::state]
 :-> (fn [{:keys [word-to-complete]}]
       (when (seq word-to-complete)
         (- (count word-to-complete)))))

(reg-sub
 ::candidates
 :<- [::state]
 :-> :candidates)
