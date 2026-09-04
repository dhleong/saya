(ns saya.modules.input.undo
  (:require
   [saya.config :as config]
   [saya.util.coll :refer [conj-with-limit]]))

(defn- buffers-unchanged? [a b]
  (identical? (:lines a)
              (:lines b)))

(defn capture-change [{:keys [buffer]}]
  (select-keys buffer [:lines :cursor]))

(defn- enqueue-undo [new-context old-context]
  (let [change (capture-change old-context)]
    (cond-> new-context
      ; Don't enqueue the same state that's already at the top
      (not (buffers-unchanged?
            change
            (peek (:undo-stack (:buffer old-context)))))
      (update-in [:buffer :undo-stack]
                 (fnil #(conj-with-limit %1 %2 config/undo-stack-size) [])
                 change))))

(defn- should-enqueue-undo? [old-context new-context]
  (and
   ; Obviously, only enqueue if we didn't modify undo stack
   (identical? (:undo-stack (:buffer old-context))
               (:undo-stack (:buffer new-context)))

   (or
     ; Enqueue on entering insert mode
    (and (= :insert (:mode new-context))
         (not= :insert (:mode old-context)))

     ; Or when making a change outside insert mode
    (and
     (not (buffers-unchanged? (:buffer old-context)
                              (:buffer new-context)))

       ; If we were already in insert mode, we've already got an enqueued undo
     (not= :insert (:mode old-context))))))

(defn maybe-enqueue-undo [new-context old-context]
  (cond-> new-context
    (should-enqueue-undo? old-context new-context)
    (enqueue-undo old-context)))
