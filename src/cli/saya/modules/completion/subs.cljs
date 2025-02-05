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
 ::word-to-complete
 :<- [::state]
 :-> :word-to-complete)

(reg-sub
 ::left-offset
 :<- [::word-to-complete]
 :-> (fn [word-to-complete]
       (when (seq word-to-complete)
         (- (count word-to-complete)))))

(reg-sub
 ::candidates
 :<- [::state]
 :-> :candidates)

(reg-sub
 ::ghost
 :<- [::left-offset]
 :<- [::candidates]
 :-> (fn [[left-offset candidates]]
       (let [ghost-offset (- left-offset)
             candidate (first candidates)]
         (when (> (count candidate) ghost-offset)
           (subs candidate ghost-offset)))))
