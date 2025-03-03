(ns saya.modules.command.subs
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-sub subscribe]]
   [saya.modules.buffers.subs :as buffer-subs]))

(reg-sub
 ::input-text
 (fn [_]
   (subscribe [::buffer-subs/by-id :cmd]))
 (fn [buffer]
   ; NOTE: There should be only one, if any
   (or (some->> (:lines buffer)
                (str/join "\n"))
       "")))
