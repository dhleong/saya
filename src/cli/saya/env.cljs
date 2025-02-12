(ns saya.env
  (:require
   ["node:fs/promises" :as fs]
   [promesa.core :as p]
   [saya.util.paths :as paths]
   [sci.core :as sci]))

(def ^:private saya-core-ns (sci/create-ns 'saya.core))

(def ^:private context-opts {:namespaces
                             {'saya.core saya-core-ns}})

(defn- read-init []
  (-> (p/let [init-path (paths/user-config "init.clj")
              buf (fs/readFile init-path)]
        (.toString buf))
      (p/catch (fn [_e]
                 ; TODO: Log e
                 nil))))

(defn- eval-fn [ctx fn-sym]
  (sci/eval-form ctx `(~fn-sym)))

(defn- maybe-eval-main [ctx ns]
  (let [main-var (sci/intern ctx ns "-main")
        main-sym (sci/var->symbol main-var)
        found-main? (some? (sci/resolve ctx main-sym))]
    (if found-main?
      [:found (eval-fn ctx main-sym)]
      [:unresolved])))

(defn- resolve-annotated [ctx ns annotation]
  (let [ns-sym (sci/ns-name ns)
        vars (sci/eval-form ctx `(ns-interns (quote ~ns-sym)))]
    (->> vars
         keys
         (filter (fn [sym]
                   (get (meta sym) annotation)))
         (map (fn [sym]
                (symbol ns-sym sym))))))

(defn- eval-script [ctx {:keys [first-load?]} script-string]
  (let [{:keys [ns]} (sci/eval-string+ ctx script-string)

        ; Check if a -main was declared
        main-result (when first-load?
                      (maybe-eval-main ctx ns))

        ; Check if any :saya/after-load hooks were declared
        after-load (when-not first-load?
                     (if-some [fns (seq
                                    (resolve-annotated
                                     ctx ns
                                     :saya/after-load))]
                       (do (doseq [after-load-fn fns]
                             (eval-fn ctx after-load-fn))
                           [:found fns])
                       [:unresolved]))]

    {:main main-result
     :after-load after-load}))

; TODO: Remove these
(def ^:private current-context (atom nil))
(def ^:private state (atom nil))

(defn initialize
  "Initialize the scripting context"
  []
  (-> (p/let [ctx (sci/init context-opts)
              init-script (read-init)]
        (when init-script
          (eval-script ctx {:first-load? true} init-script)
          (reset! current-context ctx)))
      (p/catch (fn [e]
                 ; TODO: Emit event
                 (reset! state e)))))
