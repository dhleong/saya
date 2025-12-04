(ns saya.modules.scripting.config
  (:require
   [clojure.string :as str]))

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

(defn- format-user-keymap [connr ->f user-keymap]
  ; TODO: Consider a spec?
  {:pre [(vector? user-keymap)]}
  (let [[lhs rhs opts] user-keymap
        opts (merge (when (map? rhs)
                      rhs)
                    (meta user-keymap)
                    opts)
        modes (or (get opts :modes)
                  (get opts :mode)
                  (when (:insert opts)
                    :insert)
                  "np")

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
              (->f connr rhs))

        modes (cond
                (string? modes)
                (map #(case %
                        "n" :normal
                        "p" :prompt
                        "i" :insert
                        (throw
                         (ex-info (str "Invalid mode `" % "`")
                                  {:modes modes
                                   :mode %})))
                     (str/split modes ""))

                (keyword? modes)
                [modes]

                (coll? modes)
                modes)]
    (for [mode modes]
      [mode {lhs rhs}])))

(defn format-user-keymaps
  ([connr user-keymaps] (format-user-keymaps connr rhs->callable user-keymaps))
  ([connr ->f user-keymaps]
   (->> user-keymaps
        (mapcat (partial format-user-keymap connr ->f))
        (reduce
         (fn [m [k v]]
           (update m k merge v))
         {}))))

