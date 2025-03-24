(ns saya.modules.scripting.core
  {:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
  (:require
   [archetype.util :refer [>evt]]
   [clojure.core.match :as m]
   [clojure.string :as str]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [re-frame.db :as rfdb]
   [saya.modules.echo.core :as echo-core]
   [saya.modules.kodachi.events :as kodachi]
   [saya.modules.scripting.callbacks :refer [register-callback]]
   [saya.modules.scripting.events :as events]))

(def ^:dynamic *script-file* nil)

; ======= setup-connection =================================

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

    (>evt [::events/reconfigure-connection {:connection-id connr
                                            :script-file *script-file*
                                            :keymaps keymaps}])))

(defn- current-buffer [db]
  (let [current-window (get-in db [:windows (:current-winnr db)])]
    (get-in db [:buffers (:bufnr current-window)])))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn setup-connection
  "Ensure that a connection exists in the current window for the given URI,
   configuring the associated buffer with the params provided.
   If called when a connection exists for another URI, this function is a
   nop (and any callbacks will not be called).
   "
  [uri & config]
  {:pre [(string? uri)]}
  (let [db @rfdb/app-db
        buf (current-buffer db)
        script-file *script-file*]
    ; FIXME: Probably, migrate this to an event+fx somehow? This is terribly impure
    (cond
      ; If current buffer is still connected to the same URI...
      (and (= :connected (get-in db [:connections (:connection-id buf) :state]))
           (= uri (:uri buf)))
      ; ... re-configure the active connection
      (perform-config (:connection-id buf) config)

      ; If current buffer is still connected elsewhere, reject
      (= :connected (get-in db [:connections (:connection-id buf) :state]))
      (echo-core/echo :error "Error: still connected to " (:uri buf))

      ; Else, trigger a connection
      :else
      (-> (perform-connect uri)
          (p/then #(binding [*script-file* script-file]
                     (perform-config % config)))
          (p/catch (fn [e]
                     (echo-core/echo :exception "ERROR in config: " e)))))))

; ======= Simple APIs ======================================

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn echo [& messages]
  (apply echo-core/echo messages))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn send [conn s]
  (let [text (cond
               (string? s) s
               (coll? s) (str/join "\r\n" s))]
    (>evt [:connection/send {:connr (->connr conn) :text text}])))
