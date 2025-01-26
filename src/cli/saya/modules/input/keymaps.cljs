(ns saya.modules.input.keymaps
  (:require
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

(defn perform [{:keys [bufnr winnr] :as cofx} f]
  (try
    (let [buffer (get-in cofx [:db :buffers bufnr])
          window (get-in cofx [:db :windows winnr])
          context {:buffer buffer :window window}
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

(comment
  (println (.-stack last-exception)))
