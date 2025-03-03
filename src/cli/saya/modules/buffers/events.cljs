(ns saya.modules.buffers.events
  (:require
   [re-frame.core :refer [->interceptor assoc-coeffect assoc-effect
                          get-coeffect get-effect reg-event-db unwrap]]
   [saya.modules.buffers.line :refer [ansi-continuation buffer-line]]))

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

(defn create-blank
  ([db] (create-blank db {}))
  ([db {:keys [buffer]}]
   (let [[db buffer] (allocate-buffer db (merge
                                          buffer
                                          {:lines []
                                           :cursor {:row 0 :col 0}}))
         ; TODO: Possibly reuse the current window?
         [db window] (allocate-window db {:bufnr (:id buffer)
                                          :anchor-row nil})]
     [(-> db
          (assoc :current-winnr (:id window)))

      {:buffer buffer
       :window window}])))

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
      (let [[db {buffer :buffer}] (create-blank
                                   db
                                   {:buffer
                                    {:uri uri
                                     :connection-id connection-id}})]
        (-> db
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

(defn append-text [buffer {:keys [ansi full-line? system]}]
  (update-in buffer [:lines (dec (count (:lines buffer)))]
             (fnil conj (buffer-line))
             (if system
               {:system system}
               (cond-> {:ansi ansi}
                 full-line? (assoc :full-line? true)))))

(reg-event-db
 ::append-text
 [unwrap buffer-path]
 append-text)

(defn new-line [{{cursor-row :row} :cursor :as buffer}]
  (let [prev-line (peek (:lines buffer))]
    (cond-> (update buffer :lines conj (buffer-line
                                        (when prev-line
                                          (ansi-continuation prev-line))))
      (= cursor-row
         (dec (count (:lines buffer))))
      (update-in [:cursor :row] inc))))

(reg-event-db
 ::new-line
 [unwrap buffer-path]
 (fn [buffer {:keys [system]}]
   (cond-> buffer
     ; If it's not for a system message, of course always add a
     ; new line. If it *is*, *only* add a new line if the current
     ; one isn't already blank
     (or (not system)
         (empty? (:lines buffer))
         (seq (last (:lines buffer))))
     (new-line)

     (some? system)
     (-> (append-text {:system system})
         (new-line)))))

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
