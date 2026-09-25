(ns saya.modules.layout.subs
  (:require
   [re-frame.core :refer [reg-sub subscribe]]
   [saya.modules.layout.core :as layout]))

(reg-sub
 ::by-id
 :-> :layouts)

(reg-sub
 ::with-id
 :<- [::by-id]
 :=> get)

(reg-sub
 ::current
 :<- [::with-id 0]
 :-> identity)

(reg-sub
 ::evaluated
 (fn [[_ layout-id]]
   (subscribe [::with-id layout-id]))
 :-> layout/evaluate)

(reg-sub
 ::all-keys
 :-> :layout/keys)

(reg-sub
 ::key
 :<- [::all-keys]
 :=> get)

(reg-sub
 ::script-file-data
 :<- [:script-files]
 :=> get)

(reg-sub
 ::connection-window-for-script-file
 (fn [[script-file]]
   [(subscribe [:windows])
    (subscribe [:connections])
    (subscribe [::script-file-data script-file])])
 (fn [[windows connections {:keys [connection-id]}]]
    ; TODO: This... won't work well if we support
    ; multi-window for a buffer
   (let [expected-bufnr (get connections connection-id)]
     (some
      (fn [{:keys [id bufnr]}]
        (when (= bufnr expected-bufnr)
          id))
      (vals windows)))))
