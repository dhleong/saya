(ns saya.modules.scripting.config
  (:require
   [clojure.string :as str]
   [saya.modules.scripting.keys :refer [->keys]]
   [clojure.core.match :as m]))

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
  {:pre [(or (vector? user-keymap)
             (map-entry? user-keymap))]}
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

(defn- valid-pattern? [v]
  (or (string? v)
      (regexp? v)))

(defn- format-user-trigger [connr entry]
  (letfn [(wrap-handler [f]
            (fn wrapped-handler [m]
              (f (assoc m :connr connr))))]
    (m/match [entry]
      [{:match _}] (update entry :do wrap-handler)
      [(_ :guard map?)] (let [expanded (reduce-kv
                                        (fn [m k v]
                                          (if (valid-pattern? k)
                                            (assoc m
                                                   :match k
                                                   :do (wrap-handler v))
                                            ; Option
                                            (assoc m k v)))
                                        {}
                                        entry)]
                          (when-not (:match expanded)
                            (throw (ex-info (str "Missing match clause in trigger map: " entry)
                                            {:entry entry})))
                          expanded)

      [[(pattern :guard valid-pattern?)
        (handler :guard fn?)]]
      {:match pattern
       :do (wrap-handler handler)}

      [[(pattern :guard valid-pattern?)
        (handler :guard fn?)
        (opts :guard map?)]]
      (merge opts
             {:match pattern
              :do (wrap-handler handler)}))))

(defn format-user-triggers
  ([connr user-triggers]
   (->> user-triggers
        (mapv (partial format-user-trigger connr)))))

(defn- format-user-alias
  [user-alias]
  (let [[lhs rhs] user-alias]
    (when-not (valid-pattern? lhs)
      (throw (ex-info (str "Invalid alias pattern: " lhs)
                      {:lhs lhs})))
    (cond
      (ifn? rhs) {:match lhs
                  :call rhs}
      (string? rhs) {:match lhs
                     :replace rhs})))

(defn format-user-aliases
  [user-aliases]
  (->> user-aliases
       (mapv format-user-alias)))
