(ns saya.modules.input.keymaps
  (:require
   [saya.modules.input.helpers :refer [*mode*]]
   [saya.modules.logging.core :refer [log-fx]]))

(defn- starts-with? [sequence candidate]
  (every?
   true?
   (map
    =
    sequence
    candidate)))

(defn possible? [_db keymaps keymap-buffer]
  (some
   (fn [v]
     (starts-with? v keymap-buffer))
   (keys keymaps)))

(defn build-context [{:keys [bufnr winnr] :as cofx}]
  {:buffer (get-in cofx [:db :buffers bufnr])
   :window (get-in cofx [:db :windows winnr])
   :pending-operator (get-in cofx [:db :pending-operator])})

(defn perform [{:keys [bufnr winnr] :as cofx} f]
  (try
    (let [context (build-context cofx)
          ; This merge allows us to omit unchanged fields
          context' (merge context (f context))
          _yanked (:yanked context')
          context' (dissoc context' :yanked)]
      (if-not (= context context')
        {:db (-> (:db cofx)
                 (assoc-in [:buffers bufnr] (:buffer context'))
                 (assoc-in [:windows winnr] (:window context'))
                 (dissoc :keymap-buffer :pending-operator)
                 ; TODO: Store yanked in a register, if set
                 (merge (select-keys context' [:mode :pending-operator])))
         :fx [(when-let [e (:error context')]
                (log-fx "ERROR: " e))]}

        {:db (-> (:db cofx)
                 ; Still clear this even if nothing happened:
                 (dissoc :keymap-buffer :pending-operator))}))

    (catch :default e
      ; TODO: echo?
      #_{:clj-kondo/ignore [:inline-def]}
      (def last-exception e)
      {:fx [(log-fx "ERROR performing" f ":" e)]})))

(defn maybe-perform-with-keymap-buffer [& {:keys [keymaps keymap-buffer cofx
                                                  mode with-unhandled key]
                                           :or {with-unhandled identity}}]
  (binding [*mode* mode]
    (let [new-buffer ((fnil conj []) keymap-buffer key)
          keymap (get keymaps new-buffer)
          {:keys [db]} cofx]
      (cond
        keymap
        (perform cofx keymap)

        (possible? db keymaps new-buffer)
        {:db (assoc db :keymap-buffer new-buffer)}

        :else
        (-> cofx
            (update :db dissoc :keymap-buffer :pending-operator)
            (with-unhandled)
            (select-keys [:db]))))))

(comment
  (println (.-stack last-exception)))
