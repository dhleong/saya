(ns saya.modules.input.motions.find
  (:require
   [saya.modules.buffers.util :as buffers]))

(defn perform-find-ch [{:keys [buffer] :as ctx} increment ch-pred]
  (loop [cursor (:cursor buffer)]
    (let [next-cursor (buffers/update-cursor buffer cursor increment)
          ch (buffers/char-at buffer next-cursor)]
      (if (or (and ch (ch-pred ch))
              ; We've gone as far as we can:
              (= cursor next-cursor))
        (assoc-in ctx [:buffer :cursor] next-cursor)

        (if (and (> (:row next-cursor) (:row cursor))
                 (ch-pred "\n"))
          ; Special case! Newlines can count 
          (assoc-in ctx [:buffer :cursor] next-cursor)

          (recur next-cursor))))))

(defn perform-until-ch [{:keys [buffer] :as ctx} increment ch-pred]
  (loop [cursor (:cursor buffer)]
    (let [next-cursor (buffers/update-cursor buffer cursor increment)
          ch (buffers/char-at buffer next-cursor)]
      (if (or (and ch (ch-pred ch))
              ; We've gone as far as we can:
              (= cursor next-cursor))
        (assoc-in ctx [:buffer :cursor] cursor)

        (if (and (> (:row next-cursor) (:row cursor))
                 (ch-pred "\n"))
          ; Special case! Newlines can count 
          (assoc-in ctx [:buffer :cursor] cursor)

          (recur next-cursor))))))
