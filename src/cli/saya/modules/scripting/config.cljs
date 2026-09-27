(ns saya.modules.scripting.config
  (:require
   [clojure.string :as str]
   [saya.modules.scripting.keys :refer [->keys]]))

(def ^:private core-send (delay
                           (resolve 'saya.modules.scripting.core/send)))

(defn- resolve-connr [connr]
  (if (number? connr)
    connr
    (connr)))

(defn- rhs->callable [connr {:keys [send]}]
  (fn send-callable [ctx]
    (if-some [nr (resolve-connr connr)]
      (do (@core-send nr send)
          ctx)
      {:error "No active connection"})))

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
                          (rhs (resolve-connr connr))
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
  "connr may be either a little connr or a function that
  resolves the appropriate connr. The function may return
  nil if there is no active connection"
  ([connr user-keymaps] (format-user-keymaps connr rhs->callable user-keymaps))
  ([connr ->f user-keymaps]
   (->> user-keymaps
        (mapcat (partial format-user-keymap connr ->f))
        (reduce
         (fn [m [k v]]
           (update m k merge v))
         {}))))

