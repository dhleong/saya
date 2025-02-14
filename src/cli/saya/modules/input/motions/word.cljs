(ns saya.modules.input.motions.word
  (:require
   [clojure.string :as str]
   [saya.modules.input.helpers :refer [adjust-scroll-to-cursor clamp-cursor
                                       clamp-scroll]]
   [saya.modules.input.motions.find :refer [perform-find-ch perform-until-ch]]
   [saya.modules.buffers.util :as buffers]))

(defn small-word-boundary? [ch]
  (not (re-matches #"[a-zA-Z0-9]" ch)))

(def big-word-boundary? str/blank?)

(defn word-movement [increment boundary?]
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn word-mover [ctx]
     (let [backward? (< (increment 0) 0)
           forward? (not backward?)
           start-on-boundary? (boundary? (buffers/char-at (:buffer ctx)))]
       (if forward?
         (as-> ctx ctx
           (cond-> ctx
             (not start-on-boundary?)
             (perform-find-ch increment boundary?))

           (cond-> ctx
             (or (and
                  start-on-boundary?
                  (boundary? (buffers/char-at (:buffer ctx))))
                 (str/blank? (buffers/char-at (:buffer ctx))))
             (perform-find-ch increment (complement boundary?))))

         ; backward:
         (-> ctx
             (perform-find-ch increment (complement str/blank?))

             (as-> ctx
               (cond-> ctx
                 (not (boundary? (buffers/char-at (:buffer ctx))))
                 (perform-until-ch increment boundary?)))))))))

(defn end-of-word-movement [increment boundary?]
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn end-of-word-mover [ctx]
     (let [backward? (< (increment 0) 0)
           forward? (not backward?)]
       (if forward?
         (let [ctx' (perform-until-ch ctx increment boundary?)]
           (cond-> ctx'
             (= ctx ctx') (-> (perform-find-ch increment (complement boundary?))
                              (perform-until-ch increment boundary?))))

         ; backward:
         (-> ctx
             (perform-until-ch increment boundary?)
             (perform-find-ch increment (complement boundary?))))))))
