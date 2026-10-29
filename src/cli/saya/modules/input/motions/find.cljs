(ns saya.modules.input.motions.find
  (:require
   [saya.modules.buffers.util :as buffers]))

(defn perform-find-ch
  ([ctx increment ch-pred] (perform-find-ch ctx {} increment ch-pred))
  ([{:keys [buffer] :as ctx} {:keys [require-match?] :as opts} increment ch-pred]
   (loop [cursor (:cursor buffer)]
     (let [next-cursor (buffers/update-cursor buffer opts cursor increment)
           ch (buffers/char-at buffer next-cursor)]
       (cond
         (and ch (ch-pred ch))
         (assoc-in ctx [:buffer :cursor] next-cursor)

         ; We've gone as far as we can:
         (= cursor next-cursor)
         (cond-> ctx
           ; Iff we don't require a match, we can "land on" the furthest
           ; possible cursor
           (not require-match?)
           (assoc-in [:buffer :cursor] next-cursor))

         (and (> (:row next-cursor) (:row cursor))
              (ch-pred "\n"))
         ; Special case! Newlines can count 
         (assoc-in ctx [:buffer :cursor] next-cursor)

         :else
         (recur next-cursor))))))

(defn perform-until-ch
  ([ctx increment ch-pred] (perform-until-ch ctx {} increment ch-pred))
  ([{:keys [buffer] :as ctx} {:keys [require-match?] :as opts} increment ch-pred]
   (loop [cursor (:cursor buffer)]
     (let [next-cursor (buffers/update-cursor buffer opts cursor increment)
           ch (buffers/char-at buffer next-cursor)]
       (cond
         (and ch (ch-pred ch))
         (assoc-in ctx [:buffer :cursor] cursor)

         ; We've gone as far as we can:
         (= cursor next-cursor)
         (cond-> ctx
           (not require-match?)
           (assoc-in [:buffer :cursor] cursor))

         (and (> (:row next-cursor) (:row cursor))
              (ch-pred "\n"))
         ; Special case! Newlines can count 
         (assoc-in ctx [:buffer :cursor] cursor)

         :else
         (recur next-cursor))))))
