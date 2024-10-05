(ns saya.modules.kodachi.fx
  (:require
   ["node:child_process" :as process]
   ["split2" :default split2]
   [archetype.util :refer [>evt]]
   [promesa.core :as p]
   [re-frame.core :refer [reg-fx]]
   [saya.modules.kodachi.events :as events]
   [saya.modules.logging.core :refer [log]]))

(def ^:private default-paths
  ["../kodachi/target/release/kodachi"
   "../kodachi/target/release/kodachi"])

(defonce ^:private instance (atom nil))

(defn- spawn-proc [path args opts]
  (let [promise (p/deferred)
        proc (process/spawn
              path
              (clj->js args)
              (clj->js opts))]
    (doto proc
      (.on "spawn" #(p/resolve! promise proc))
      (.on "error" #(p/reject! promise %)))
    promise))

(defn- parse-message [raw-message]
  (log "< " raw-message)
  (js/JSON.parse raw-message))

(defn- spawn-kodachi [path]
  (-> (p/let [^js proc (spawn-proc path
                                   ["stdio" "--ui=external"]
                                   {:stdio ["pipe" nil "pipe"]
                                    :windowsHide true})]
        (swap! instance
               (fn [^js old]
                 (when old
                   (.kill old))
                 proc))

        (doto
         (-> proc
             .-stderr
             (.pipe (split2 parse-message)))

          (.on "data" (fn [msg]
                        (>evt [::events/on-message msg])))
          (.on "error" (fn [err]
                         (log "Error from kodachi:" err)
                         (>evt [::events/on-error err]))))

        :started)

      (p/catch identity)))

(defn- init []
  (p/loop [paths default-paths
           last-err nil]
    (if-let [path (first paths)]
      (p/let [result (spawn-kodachi path)]
        (if (= :started result)
          (>evt [::events/ready])

          (p/recur (next paths)
                   result)))

      ; No executable found
      ; TODO: Maybe try to install?
      (throw (or last-err
                 (ex-info "Unable to find valid kodachi install" {}))))))

(defn- serialize-message [message]
  (-> message
      (clj->js)
      (js/JSON.stringify)))

(defn- send! [message]
  (if-some [^js proc @instance]
    (doto (.-stdin proc)
      (.write (serialize-message message))
      (.write "\n"))

    (throw (ex-info "Attempting to send! when uninitialized" {}))))

(reg-fx
 ::init
 (fn [_db]
   (>evt [::events/initializing])

   (-> (init)
       (p/catch (fn [e]
                  ; Clean up the bad instance on error
                  (swap! instance
                         (fn [^js v]
                           (when v
                             (.kill v))
                           nil))
                  (>evt [::events/unavailable e]))))))

(reg-fx
 ::send!
 send!)
