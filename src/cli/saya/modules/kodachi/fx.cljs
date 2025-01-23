(ns saya.modules.kodachi.fx
  (:require
   ["node:child_process" :as process]
   ["split2" :default split2]
   [applied-science.js-interop :as j]
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
  (-> raw-message
      (js/JSON.parse)
      (js->clj :keywordize-keys true)))

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
                        (when-let [id (:request_id msg)]
                          (log "EMIT: " (str "response:" id))
                          (.emit proc
                                 (str "response:" id)
                                 msg))
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

(defn- dispatch! [message]
  (if-some [^js proc @instance]
    (doto (.-stdin proc)
      (.write (serialize-message message))
      (.write "\n"))

    (throw (ex-info "Attempting to send! when uninitialized" {}))))

(defn- generate-next-request-id []
  (let [^js proc @instance]
    (when-not proc
      (throw (ex-info "Generating ID for dead daemon" {})))

    (let [id (j/get proc :next-request-id 0)]
      (j/update! proc :next-request-id (fnil inc 0))
      id)))

(defn- await-response [id]
  (let [^js proc @instance]
    (when-not proc
      (throw (ex-info "Awaiting response from dead daemon"
                      {:request-id id})))

    (p/create
     (fn [resolve]
       (.once proc
              (str "response:" id)
              (fn [m]
                (log "GOT RESPONSE TO " id m)
                (resolve m)))))))

(defn- request! [message]
  (let [next-id (generate-next-request-id)]
    (dispatch! (assoc message :id next-id))
    (await-response next-id)))

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
 ::dispatch!
 dispatch!)

(reg-fx
 ::connect!
 (fn [{:keys [uri]}]
   (p/let [{:keys [connection_id]} (request! {:type :Connect
                                              :uri uri})]
     (log "Opened connection" connection_id "to" uri)
     (>evt [::events/connecting {:uri uri
                                 :connection-id connection_id}]))))

(reg-fx
 ::disconnect!
 (fn [{:keys [connection-id]}]
   (request! {:type :Disconnect
              :connection_id connection-id})))

(reg-fx
 ::send!
 (fn [{:keys [connection-id text]}]
   (request! {:type :Send
              :connection_id connection-id
              :text text})))
