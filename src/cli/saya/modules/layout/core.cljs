(ns saya.modules.layout.core
  (:require
   [clojure.core.match :as m]
   [clojure.zip :as zip :refer [zipper]]
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

(defn- unpack-evaluate-layout [context parent-key component-type args]
  (let [[opts args] (unpack-component-args args)]
    (into [component-type opts]
          (map-indexed
           (fn [i v]
             (evaluate-form context (conj parent-key i) v))
           args))))

(defn- evaluate-form [context parent-key [component & args]]
  (case component
    :horizontal (unpack-evaluate-layout
                 context
                 (conj parent-key :horizontal)
                 components/horizontal args)
    :vertical (unpack-evaluate-layout
               context
               (conj parent-key :vertical)
               components/vertical args)
    :edit (let [k (conj parent-key (key-for-params (first args)))
                props (merge context {:key k})]
            (with-meta
              (m/match [(first args)]
                [{:file path}] [components/edit-file-view props path]
                [{:content (s :guard string?)}] [components/edit-string-view props s]
                [{:content (s :guard coll?)}] [components/edit-string-view props s]
                [{:content s}] [components/edit-ref-view props s])
              {:key k}))))

(defn evaluate [{:keys [id component script-file]}]
  (let [rendered (component)]
    (evaluate-form
     {:script-file script-file}
     [id]
     rendered)))

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
    :edit (let [{:keys [focus file] :as params} (first args)
                child-key (key-for-params params)
                full-key (conj parent-key child-key)
                existing-mapping (get-in db [:layout/keys full-key])]
            (if existing-mapping
              db
              (let [[db {:keys [buffer window]}]
                    (create-blank
                     db
                     {:focus? focus
                      :buffer
                      (when-not file
                        {:flags #{:readonly}})})]
                (->
                 db
                 (assoc-in [:layout/keys full-key]
                           {:bufnr (:id buffer)
                            :winnr (:id window)})
                 (assoc-in [:layout/lookup-keys :bufnr (:id buffer)]
                           full-key)
                 (assoc-in [:layout/lookup-keys :winnr (:id buffer)]
                           full-key)))))))

(defn install [db {:keys [id component]}]
  (let [rendered (component)]
    (install-form db [id] rendered)))

; ======= Navigate =========================================

(defn- hiccup-zip [hic]
  (zipper
    ; TODO: *probably* check that the component
    ; is a layout component
   vector?
   #(subvec % 2)
   (fn [node children] (with-meta
                         (vec children)
                         (meta node)))
   hic))

(defn- nth-child-node [loc n]
  (reduce
   (fn [l' _]
     (zip/right l'))
   (zip/down loc)
   (range n)))

(defn zipper-at-key [evaluated-layout at-key]
  (loop [zipper (hiccup-zip evaluated-layout)
         ; The first element is the root layout ID
         [_layout-kind idx & remaining] (next at-key)]
    (if (some? idx)
      (recur
       (nth-child-node zipper idx)
       remaining)
      zipper)))

(defn zipper-key [loc]
  (:key (second (zip/node loc))))

(defn zipper-component [loc]
  (first (zip/node loc)))

(defn find-sibling-in-ancestors [loc axis zipper-next]
  {:pre [(#{:horizontal :vertical} axis)]}
  (when loc
    (let [expected-component (case axis
                               :horizontal components/horizontal
                               :vertical components/vertical)]
      (loop [loc loc]
        (when-let [parent (zip/up loc)]
          (if (and (identical? (zipper-component parent)
                               expected-component)
                   (some? (zipper-next loc)))
            (zipper-next loc)
            (recur parent)))))))

; TODO: These need to take into account the cursor
; position and the actual size of windows to more
; precisely navigate
(defn navigate-axis [loc axis zipper-next]
  {:pre [(#{:horizontal :vertical} axis)]}
  ; The algorithm is:
  ; 1. Recurse upward until we find a zipper-next sibling,
  ;    or reach the root
  ; 2. If we found a sibling, recurse down into it
  ;    until we find a leaf
  (loop [loc (find-sibling-in-ancestors loc axis zipper-next)]
    (when loc
      (if (some? (zipper-key loc))
        loc
        (recur (zip/down loc))))))

(defn navigate-left [loc]
  (navigate-axis loc :horizontal zip/left))

(defn navigate-right [loc]
  (navigate-axis loc :horizontal zip/right))

(defn navigate-up [loc]
  (navigate-axis loc :vertical zip/left))

(defn navigate-down [loc]
  (navigate-axis loc :vertical zip/right))
