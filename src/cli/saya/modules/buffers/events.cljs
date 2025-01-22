(ns saya.modules.buffers.events
  (:require
   [day8.re-frame-10x.inlined-deps.re-frame.v1v3v0.re-frame.core :refer [get-effect]]
   [re-frame.core :refer [->interceptor assoc-coeffect assoc-effect
                          get-coeffect reg-event-db unwrap]]))

(defn- build-allocator [db-objs-key db-next-id-key]
  (fn allocate [db extras]
    (let [id (db-next-id-key db)
          obj (merge extras {:id id})]
      [(-> db
           (update db-next-id-key (fnil inc 0))
           (assoc-in [db-objs-key id] obj))
       obj])))

(def ^:private allocate-buffer (build-allocator :buffers :next-bufnr))
(def ^:private allocate-window (build-allocator :windows :next-winnr))

(def ^:private buffer-path
  (->interceptor
   :id :buffer-path
   :before (fn [context]
             (when (:re-frame.db/path-history context)
               (throw (ex-info "buffer-path is not compatible with path"
                               {:context context})))

             (let [[_ {:keys [id]}] (get-coeffect context :original-event)
                   original-db (get-coeffect context :db)]
               (-> context
                   (assoc ::original-db original-db)
                   (assoc ::buffer-path [:buffers id])
                   (assoc-coeffect :db (get-in original-db [:buffers id])))))
   :after (fn [context]
            (let [original-db  (::original-db context)
                  context' (-> (dissoc context ::original-db
                                       ::buffer-path)
                               (assoc-coeffect :db original-db))     ;; put the original db back so that things like debug work later on
                  db (get-effect context :db ::not-found)
                  path (::buffer-path context)]
              (if (= db ::not-found)
                context'
                (->> (assoc-in original-db path db)
                     (assoc-effect context' :db)))))))

(defn- create-for-connection [db {:keys [connection-id uri]}]
  (let [current-winnr (:current-winnr db)
        current-window (get-in db [:windows current-winnr])
        current-path [:buffers (:bufnr current-window)]
        current-buffer (get-in db current-path)]
    (if (= (:uri current-buffer) uri)
      ; TODO: Reuse existing buffer
      (update-in db current-path assoc :connection-id connection-id)

      ; TODO: Also, tabpage
      (let [[db buffer] (allocate-buffer db {:uri uri
                                             :connection-id connection-id
                                             :lines []
                                             :cursor {:x 0 :y 0}})
            [db window] (allocate-window db {:bufnr (:id buffer)})]
        (-> db
            (assoc :current-winnr (:id window))
            (update :connection->bufnr
                    assoc
                    connection-id
                    (:id buffer)))))))

(reg-event-db
 ::create-for-connection
 [unwrap]
 create-for-connection)

(defn append-text [buffer {:keys [ansi parsed]}]
  (update-in buffer [:lines (dec (count (:lines buffer)))]
             (fnil conj [])
             {:ansi ansi
              :parsed parsed}))

(reg-event-db
 ::append-text
 [unwrap buffer-path]
 append-text)

(defn new-line [buffer]
  (update buffer :lines conj []))

(reg-event-db
 ::new-line
 [buffer-path]
 (fn [buffer _]
   (new-line buffer)))
