(ns saya.modules.input.motions.word
  (:require
   [clojure.string :as str]
   [saya.modules.input.helpers :refer [adjust-scroll-to-cursor clamp-cursor
                                       clamp-scroll]]
   [saya.modules.input.motions.find :refer [perform-find-ch perform-until-ch]]))

(defn small-word-boundary? [ch]
  (re-matches #"[a-zA-Z0-9]" ch))

(defn word-movement [increment ch-pred]
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn word-mover [ctx]
     (let [backwards? (< (increment 0) 0)]
       (cond-> ctx
         (not backwards?) (perform-find-ch increment str/blank?)
         :always (perform-find-ch increment ch-pred)
         backwards? (perform-until-ch increment (complement ch-pred)))))))
