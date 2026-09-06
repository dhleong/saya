(ns saya.modules.layout.core
  (:require
   [clojure.core.match :as m]
   [saya.modules.buffers.events :refer [create-blank]]
   [saya.modules.layout.components :as components]))

(defn- unpack-component-args [args]
  (if (map? (first args))
    [(first args) (next args)]
    [nil args]))

(defn- key-for-params [params]
  (m/match [params]
    [{:key k}] k
    [{:file path}] {:file path}
    [{:content (_ :guard string?)}] :string
    [{:content (_ :guard coll?)}] :coll
    [_] :ref))

; ======= evaluate =========================================

(declare ^:private evaluate-form)

(defn- unpack-evaluate-layout [parent-key component-type args]
  (let [[opts args] (unpack-component-args args)]
    (into [component-type opts]
          (map-indexed
           (fn [i v]
             (evaluate-form (conj parent-key i) v))
           args))))

(defn- evaluate-form [parent-key [component & args]]
  (case component
    :horizontal (unpack-evaluate-layout (conj parent-key :horizontal)
                                        components/horizontal args)
    :vertical (unpack-evaluate-layout (conj parent-key :vertical)
                                      components/vertical args)
    :edit (let [k (conj parent-key (key-for-params (first args)))]
            (with-meta
              (m/match [(first args)]
                [{:file path}] [components/edit-file-view {:key k} path]
                [{:content (s :guard string?)}] [components/edit-string-view {:key k} s]
                [{:content (s :guard coll?)}] [components/edit-string-view {:key k} s]
                [{:content s}] [components/edit-ref-view {:key k} s])
              {:key k}))))

(defn evaluate [{:layout/keys [id component state-atom]}]
  (let [rendered (component @state-atom)]
    (evaluate-form [id] rendered)))

; ======= install ==========================================

(declare ^:private install-form)

(defn- install-layout-part [db parent-key args]
  (let [[_ children] (unpack-component-args args)]
    (reduce
     (fn [db' [i child]]
       (install-form db' (conj parent-key i) child))
     db
     (map-indexed vector children))))

(defn- install-form [db parent-key [component & args]]
  (case component
    :horizontal (install-layout-part db (conj parent-key :horizontal) args)
    :vertical (install-layout-part db (conj parent-key :vertical) args)
    :edit (let [child-key (key-for-params (first args))
                full-key (conj parent-key child-key)
                existing-mapping (get-in db [:layout/keys full-key])]
            (if existing-mapping
              db
              (let [[db {:keys [buffer window]}] (create-blank db {:foucs? false})]
                (assoc-in db [:layout/keys full-key] {:bufnr (:id buffer)
                                                      :winnr (:id window)}))))))

(defn install [db {:layout/keys [id component state-atom]}]
  (let [rendered (component @state-atom)]
    (install-form db [id] rendered)))
