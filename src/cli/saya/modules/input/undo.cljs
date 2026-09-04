(ns saya.modules.input.undo
  (:require
   [saya.config :as config]
   [saya.util.coll :refer [conj-with-limit]]))

(defn capture-change [{:keys [buffer]}]
  (select-keys buffer [:lines :cursor]))

(defn- enqueue-undo [new-context old-context]
  (update-in new-context [:buffer :undo-stack]
             (fnil #(conj-with-limit %1 %2 config/undo-stack-size) [])
             (capture-change old-context)))

(defn- should-enqueue-undo? [old-context new-context]
  (and
   (not (:did-undo? new-context))
   (not (identical? (:lines (:buffer old-context))
                    (:lines (:buffer new-context))))
   ; If we were already in insert mode, we've already got an enqueued undo
   (not= :insert (:mode old-context))))

(defn maybe-enqueue-undo [new-context old-context]
  (cond-> new-context
    (should-enqueue-undo? old-context new-context)
    (enqueue-undo old-context)))
