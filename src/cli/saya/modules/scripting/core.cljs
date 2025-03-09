(ns saya.modules.scripting.core
  (:require
   [archetype.util :refer [>evt]]
   [clojure.core.match :as m]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [saya.modules.kodachi.events :as kodachi]
   [saya.modules.logging.core :refer [log]]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn on-connection
  "Ensure that a connection exists in the current window for the given URI,
   calling the given callback when it's connected (and again if the script is
   sourced while the connection is active).
   If called when a connection exists for another URI, this function is a
   nop (and the callback will not be called).
   "
  [uri f]
  ; TODO: First, check if we're already actively connected. If so,
  ; call f directly

  ; Else, trigger a connection
  (let [callback-id [::on-connection uri]
        my-connr (atom nil)
        stop-listening (fn stop-listening []
                         (rf/remove-post-event-callback callback-id))
        handle-error (fn handle-error [e]
                       ; TODO: echo
                       (log "ERROR: " e))]
    (rf/add-post-event-callback
     (fn [[event-name & args] _]
       (m/match [event-name (vec args)]
         [::kodachi/connecting [{:connection-id connection-id
                                 :uri uri}]]
         (reset! my-connr connection-id)

         [::kodachi/on-message [{:type "Connected"
                                 :connection_id connr}]]
         (-> (p/do
               (f connr))
             (p/catch handle-error)
             (p/finally stop-listening))

         [::kodachi/on-message [{:type "Disconnected"
                                 :connection_id connr}]]
         (when (= connr @my-connr)
           (stop-listening))

         [::kodachi/on-error _]
         (stop-listening)

         :else nil)))

    (>evt [:command/connect {:uri uri}])))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn ^:sci/macro with-connection [_&form _&env uri & body]
  `(saya.core/on-connection
    ~uri
    (fn [connection#]
      (p/do
        ~@body))))

(defn keymap-set [conn mode lhs rhs]
  ; TODO:
  )

(defn send [conn s]
  ; TODO:
  )
