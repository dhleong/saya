(ns saya.modules.input.keymaps
  (:require
   [saya.modules.input.helpers :refer [*mode*]]
   [saya.modules.input.normal :as normal]
   [saya.modules.logging.core :refer [log-fx]]))

(defn- starts-with? [sequence candidate]
  (every?
   true?
   (map
    =
    sequence
    candidate)))

(defn possible? [_db mode keymap-buffer]
  (some
   (fn [v]
     (starts-with? v keymap-buffer))
   (case mode
     :normal (keys normal/keymaps)
     #{})))

(defn build-context [{:keys [bufnr winnr] :as cofx}]
  (let [buffer (get-in cofx [:db :buffers bufnr])
        window (get-in cofx [:db :windows winnr])]
    {:buffer buffer :window window}))

(defn perform [{:keys [bufnr winnr] :as cofx} f]
  (try
    (let [context (build-context cofx)
          ; This merge allows us to omit unchanged fields
          context' (merge context (f context))]
      (when-not (= context context')
        {:db (-> (:db cofx)
                 (assoc-in [:buffers bufnr] (:buffer context'))
                 (assoc-in [:windows winnr] (:window context'))
                 (dissoc :keymap-buffer))}))
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

        (possible? db :insert new-buffer)
        {:db (assoc db :keymap-buffer new-buffer)}

        :else
        (-> cofx
            (update :db dissoc :keymap-buffer)
            (with-unhandled)
            (select-keys [:db]))))))

(comment
  (println (.-stack last-exception)))
