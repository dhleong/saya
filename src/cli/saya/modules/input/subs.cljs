(ns saya.modules.input.subs
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::keymap-buffer
 :-> :keymap-buffer)

(reg-sub
 ::consumed-keymap-buffer
 :-> :consumed-keymap-buffer)

(reg-sub
 ::pending-getkey
 :-> :pending-getkey)

(reg-sub
 ::keymap-buffer-string
 :<- [:mode]
 :<- [::pending-getkey]
 :<- [::consumed-keymap-buffer]
 :<- [::keymap-buffer]
 :-> (fn [[mode pending-getkey consumed keymap]]
       ; NOTE: Vim shows the "activating" key briefly. Handling that
       ; would probably be a pain, so skipping it is an 80/20
       (when (or (seq keymap)
                 (seq pending-getkey)
                 (= :operator-pending mode))
         (->> (concat consumed keymap)
              (str/join)))))
