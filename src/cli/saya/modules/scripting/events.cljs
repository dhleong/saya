(ns saya.modules.scripting.events
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-db unwrap]]))

(def ^:private core-send (delay
                           (resolve 'saya.modules.scripting.core/send)))

(defn- rhs->callable [connr {:keys [send]}]
  (fn send-callable [ctx]
    (@core-send connr send)
    ctx))

(defn- ->keys [v]
  (cond
    (string? v) (str/split v "")
    (vector? v) v
    :else (throw (ex-info (str "Invalid keys value: `" v "`")
                          {:v v}))))

(defn- format-user-keymap [connr user-keymap]
  ; TODO: Consider a spec?
  {:pre [(vector? user-keymap)]}
  (let [[lhs rhs opts] user-keymap
        opts (merge (meta user-keymap) opts)
        modes (get opts :modes "n")

        lhs (->keys lhs)

        rhs (cond
              ; TODO: Support remaps?
              (string? rhs)
              (throw
               (ex-info (str "Invalid rhs for mapping `" lhs "`")
                        {:rhs rhs}))

              (fn? rhs) (fn [ctx]
                          (rhs connr)
                          ctx)

              (and (map? rhs)
                   (:send rhs))
              (rhs->callable connr rhs))

        ; TODO: !!!
        modes (case modes
                "n" :normal
                modes)]
    ; TODO: multi-mode mappings
    [modes {lhs rhs}]))

(defn- format-user-keymaps [connr user-keymaps]
  (->> user-keymaps
       (map (partial format-user-keymap connr))
       (reduce
        (fn [m [k v]]
          (update m k merge v))
        {})))

(reg-event-db
 ::reconfigure-connection
 [unwrap]
 (fn [db {:keys [connection-id script-file keymaps]}]
   (let [bufnr (get-in db [:connections connection-id :bufnr])]
     (-> db
         (assoc-in [:connections connection-id :script-file] script-file)
         (assoc-in [:buffers bufnr :keymaps] (format-user-keymaps
                                              connection-id
                                              keymaps))))))
