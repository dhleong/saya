(ns saya.modules.buffers.events
  (:require
   [re-frame.core :refer [->interceptor assoc-coeffect assoc-effect
                          get-coeffect get-effect reg-event-db unwrap]]))

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

(defn create-for-connection [db {:keys [connection-id uri]}]
  (let [current-winnr (:current-winnr db)
        current-window (get-in db [:windows current-winnr])
        current-path [:buffers (:bufnr current-window)]
        current-buffer (get-in db current-path)]
    (if (= (:uri current-buffer) uri)
      ; Reuse existing buffer
      (-> db
          (update-in current-path assoc :connection-id connection-id)
          (update :connection->bufnr
                  assoc
                  connection-id
                  (:id current-buffer)))

      ; TODO: Also, tabpage
      (let [[db buffer] (allocate-buffer db {:uri uri
                                             :connection-id connection-id
                                             :lines []
                                             :cursor {:row 0 :col 0}})
            [db window] (allocate-window db {:bufnr (:id buffer)
                                             :anchor-row nil})]
        (-> db
            (assoc :current-winnr (:id window))
            (update :connection->bufnr
                    assoc
                    connection-id
                    (:id buffer)))))))

; NOTE: Adapters like kodachi.events can use the event handler directly, assuming they use [unwrap]
; The handler expects the db as its first param; be aware if your event handler is -fx!
; (reg-event-db
;  ::create-for-connection
;  [unwrap]
;  create-for-connection)

(defn append-text [buffer {:keys [ansi parsed full-line? system]}]
  (update-in buffer [:lines (dec (count (:lines buffer)))]
             (fnil conj [])
             (if system
               {:system system}
               (cond-> {:ansi ansi :parsed parsed}
                 full-line? (assoc :full-line? true)))))

(reg-event-db
 ::append-text
 [unwrap buffer-path]
 append-text)

(defn new-line [{{cursor-row :row} :cursor :as buffer}]
  (cond-> (update buffer :lines conj [])
    (= cursor-row
       (dec (count (:lines buffer))))
    (update-in [:cursor :row] inc)))

(reg-event-db
 ::new-line
 [unwrap buffer-path]
 (fn [buffer {:keys [system]}]
   (cond-> (new-line buffer)
     system (append-text {:system system}))))

(defn- clear-line [buffer]
  (-> buffer
      (update :lines pop)
      (new-line)))

(defn clear-partial-line [buffer]
  (let [last-line (last (:lines buffer))
        {:keys [full-line?]} (last last-line)]
    (cond-> buffer
      (not full-line?)
      (clear-line))))

(reg-event-db
 ::clear-partial-line
 [buffer-path]
 (fn [buffer _]
   (clear-partial-line buffer)))

(comment
  (re-frame.core/dispatch [::clear-partial-line {:id 0}])
  (re-frame.core/dispatch [::append-text {:id 0 :ansi "Hi there"}])

  (re-frame.core/dispatch
   [::new-line {:id 0 :system [:disconnected]}]))
