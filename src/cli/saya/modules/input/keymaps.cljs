(ns saya.modules.input.keymaps
  (:require
   [saya.modules.echo.core :refer [echo-fx]]
   [saya.modules.echo.events :as echo-events]
   [saya.modules.input.helpers :refer [*mode*]]))

(defn- starts-with? [sequence candidate]
  (and
   (>= (count sequence)
       (count candidate))
   (every?
    true?
    (map
     =
     sequence
     candidate))))

(defn possible? [keymaps keymap-buffer]
  (some
   (fn [v]
     (starts-with? v keymap-buffer))
   (keys keymaps)))

(defn build-context [{:keys [bufnr connr winnr] :as cofx}]
  {:buffer (get-in cofx [:db :buffers bufnr])
   :normal-buffer (get-in cofx [:db :buffers (:normal-bufnr cofx)])
   :window (get-in cofx [:db :windows winnr])
   :editable (when-not (= bufnr [:conn/input connr])
               (get-in cofx [:db :buffers [:conn/input connr]]))
   :pending-operator (get-in cofx [:db :pending-operator])
   :search (select-keys (get-in cofx [:db :search])
                        [:direction :query])
   :histories (get-in cofx [:db :histories])})

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
                 (merge (select-keys context' [:mode :pending-operator]))
                 (cond->
                  (:editable context')
                   (assoc-in [:buffers (:id (:editable context'))]
                             (:editable context'))

                   (:normal-buffer context')
                   (assoc-in [:buffers (:id (:normal-buffer context'))]
                             (:normal-buffer context'))))
         :fx [(when-let [e (:error context')]
                (echo-fx :exception "ERROR:" e))

              (when (:mode context')
                [:dispatch [::echo-events/ack-echo]])]}

        {:db (-> (:db cofx)
                 ; Still clear this even if nothing happened:
                 (dissoc :keymap-buffer :pending-operator))}))

    (catch :default e
      #_{:clj-kondo/ignore [:inline-def]}
      (def last-exception e)
      {:fx [(echo-fx :exception "ERROR performing" f ":" e)]})))

(defn maybe-perform-with-keymap-buffer [& {:keys [keymaps keymap-buffer cofx
                                                  mode with-unhandled key]
                                           :or {with-unhandled identity}}]
  (binding [*mode* mode]
    (let [new-buffer ((fnil conj []) keymap-buffer key)
          {:keys [db bufnr normal-bufnr]} cofx
          user-maps (or (get-in db [:buffers bufnr :keymaps mode])
                        (get-in db [:buffers normal-bufnr :keymaps mode]))
          combined-maps (merge keymaps user-maps)
          keymap (get combined-maps new-buffer)]
      (cond
        keymap
        (perform cofx keymap)

        (possible? combined-maps new-buffer)
        {:db (assoc db :keymap-buffer new-buffer)}

        :else
        (-> cofx
            (update :db dissoc :keymap-buffer :pending-operator)
            (with-unhandled)
            (select-keys [:db]))))))

(comment
  (println (.-stack last-exception)))
