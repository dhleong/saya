(ns saya.modules.scripting.core
  {:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
  (:require
   [archetype.util :refer [>evt]]
   [clojure.core.match :as m]
   [clojure.string :as str]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [saya.modules.kodachi.events :as kodachi]
   [saya.modules.logging.core :refer [log]]
   [saya.modules.scripting.callbacks :refer [register-callback]]))

(defn- perform-connect [uri]
  (let [callback-id [::on-connection uri]
        stop-listening (fn stop-listening []
                         (rf/remove-post-event-callback callback-id))]
    (p/create
     (fn [p-resolve _]
       (rf/add-post-event-callback
        callback-id
        (fn [[event-name & args] _]
          (m/match [event-name (vec args)]
            [::kodachi/connecting [{:connection-id connection-id
                                    :uri uri}]]
            (do (stop-listening)
                (p-resolve connection-id))

            :else nil)))

       (>evt [:command/connect {:uri uri}])))))

; "Unpacks" a `conn` into a connr. For now, a no-op
(def ^:private ->connr identity)

(defn- perform-config [conn {keymaps :keys :as config}]
  (let [connr (->connr conn)]
    (register-callback connr (fn [kind]
                               (when-some [cb (get config kind)]
                                 (cb conn))))

    ; TODO: 
    (log "TODO: map keys: " keymaps " for " connr)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn setup-connection
  "Ensure that a connection exists in the current window for the given URI,
   configuring the associated buffer with the params provided.
   If called when a connection exists for another URI, this function is a
   nop (and any callbacks will not be called).
   "
  [uri & config]
  ; TODO: First, check if we're already actively connected. If so,
  ; perform config directly

  ; Else, trigger a connection
  (-> (perform-connect uri)
      (p/then (partial perform-config config))
      (p/catch (fn [e]
                 ; TODO: Echo
                 (log "ERROR in config: " e)))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn send [conn s]
  (let [text (cond
               (string? s) s
               (coll? s) (str/join "\r\n" s))]
    (>evt [:connection/send {:connr (->connr conn) :text text}])))
