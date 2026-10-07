(ns saya.modules.kodachi.fx
  (:require
   [archetype.util :refer [>evt]]
   [clojure.core.match :as m]
   [promesa.core :as p]
   [re-frame.core :refer [reg-fx]]
   [saya.modules.echo.core :refer [echo]]
   [saya.modules.kodachi.api :as api]
   [saya.modules.kodachi.events :as events]
   [saya.modules.logging.core :refer [log]]))

(reg-fx
 ::init
 (fn [_db]
   (>evt [::events/initializing])

   (-> (api/init)
       (p/catch (fn [e]
                  (>evt [::events/unavailable e]))))))

(reg-fx
 ::connect!
 (fn [{:keys [uri] :as payload}]
   (p/let [{:keys [connection_id] :as opts} (api/request!
                                             (merge
                                              payload
                                              {:type :Connect}))]
     (log "Opened connection" connection_id "to" uri)
     (>evt [::events/connecting {:uri uri
                                 :connection-id connection_id
                                 :opts opts}])
     (log "Queued ::connecting"))))

(reg-fx
 ::disconnect!
 (fn [{:keys [connection-id]}]
   (api/request! {:type :Disconnect
                  :connection_id connection-id})))

(reg-fx
 ::send!
 (fn [{:keys [connection-id text persist?]}]
   (api/request! {:type :Send
                  :connection_id connection-id
                  :text text
                  :persist persist?})))

(reg-fx
 ::set-window-size!
 (fn [{:keys [connection-id width height]}]
   (when (and width height)
     (api/dispatch! {:type :WindowSize
                     :connection_id connection-id
                     :width width
                     :height height}))))

(defn- load-persisted-range!
  [{:keys [bufnr key start end]}]
  (p/let [{:keys [lines]} (api/request! {:type :GetPersistedOutput
                                         :key key
                                         :start_line start
                                         :end_line end})]
    (>evt [::events/on-persisted-range-loaded
           {:bufnr bufnr
            :start start
            :end end
            :lines lines}])))

(reg-fx
 ::load-persisted-range!
 load-persisted-range!)

(defonce ^:private enqueued-ranges (atom {}))

(defn- flush-load! [bufnr key]
  (let [path [bufnr key]
        [old _] (swap-vals! enqueued-ranges dissoc path)
        request (merge
                 (select-keys
                  (get old path)
                  [:start :end])
                 {:bufnr bufnr
                  :key key})]
    (load-persisted-range! request)))

(reg-fx
 ::enqueue-load-persisted-line!
 (fn [{:keys [bufnr key idx]}]
   (swap!
    enqueued-ranges
    update
    [bufnr key]
    (fn [state]
      (-> state
          (update :start (fnil min idx) idx)
          (update :end (fnil max idx) idx)
          (cond->
           (nil? (:timeout state))
            (assoc :timeout (js/setTimeout
                             (partial flush-load! bufnr key)
                             50))))))))

(defn- format-matcher [match]
  (m/match [match]
    [(_ :guard regexp?)]
    {:type :Regex
     :source (.-source ^js match)}

    [(_ :guard string?)]
    {:type :Simple
     :source match}))

(reg-fx
 ::configure-connection!
 (fn [{:keys [connr aliases triggers]}]
   (p/do
     (api/dispatch! {:type :Clear
                     :connection_id connr})

     (p/doseq [[id {:keys [match] :as alias}] (map-indexed vector aliases)]
       (-> (api/request!
            (merge
             {:type "RegisterAlias"
              :connection_id connr
              :matcher (format-matcher match)}
             (m/match [alias]
               [{:call _handler}] {:handler_id id}
               [{:replace rhs}] {:replacement_pattern rhs})))))

     (p/doseq [[id {:keys [match consume?]}] (map-indexed vector triggers)]
       (-> (api/request!
            {:type :RegisterTrigger
             :connection_id connr
             :handler_id id
             :matcher (merge
                       (when consume?
                         {:consume consume?})
                       (format-matcher match))})
           (p/catch (fn [e]
                      (echo :error "Error registering trigger: " e))))))))
