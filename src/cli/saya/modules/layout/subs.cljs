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

(reg-sub ::script-files :-> :script-files)

(reg-sub
 ::script-file-data
 :<- [::script-files]
 :=> get)

(reg-sub
 ::connection-window-for-script-file
 (fn [[_ script-file]]
   [(subscribe [:windows])
    (subscribe [:connections])
    (subscribe [::script-file-data script-file])])
 (fn [[windows connections {:keys [connection-id]}]]
    ; TODO: This... won't work well if we support
    ; multi-window for a buffer
   (let [expected-bufnr (get-in connections
                                [connection-id :bufnr])]
     (some
      (fn [{:keys [id bufnr]}]
        (when (= bufnr expected-bufnr)
          id))
      (vals windows)))))

(reg-sub
 :layout/lookup-keys
 :-> :layout/lookup-keys)

(reg-sub
 ::lookup-keys-by-type
 :<- [:layout/lookup-keys]
 :=> get)

(reg-sub
 ::connection-key-for-script-file
 :<- [::lookup-keys-by-type :script-file/connection]
 :=> get)
