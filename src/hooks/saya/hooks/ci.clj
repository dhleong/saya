(ns saya.hooks.ci
  (:require
   [clojure.java.io :refer [file make-parents]]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [shadow.build.classpath :as cp]
   [shadow.build.data :as data]))

(defn ^:export generate-tests-registry
  "Generates src/target/saya/ci.cljs for running all tests"
  {:shadow.build/stage :compile-prepare}
  ([build-state] (generate-tests-registry build-state #"-test$"))
  ([build-state ns-regexp]
   (let [re (cond-> ns-regexp
              (string? ns-regexp) (re-pattern))
         syms (->> build-state
                   :sym->id
                   keys
                   (filter (fn [s]
                             (some? (re-find re (str s))))))

         registry-name "saya/ci.cljs"
         registry-path "target/ci-src/saya/ci.cljs"
         resource-id [:shadow.build.classpath/resource registry-name]
         src (str "
(ns saya.ci
 (:require
 [cljs.test :as test]\n"
                  (->> syms
                       (map (fn [s] (str "[" s "]")))
                       (str/join "\n"))
                  "  ))

(defmethod test/report [::test/default :end-run-tests]
  [{:keys [fail error]}]
  (js/process.exit (min (+ fail error) 1)))

(test/run-all-tests #\".*-test$\")")

         resource (do
                    ; Create the file before trying to make a resource of it
                    (make-parents registry-path)
                    (spit registry-path src)

                    (-> (cp/make-fs-resource
                         (.getAbsoluteFile (file registry-path))
                         registry-name)
                        (assoc
                         :type :cljs
                         :output-name "saya.ci.js"
                         :provides #{}
                         :requires (into #{} syms)
                         :deps (into [] syms))))]

     (println syms)
     (println (s/explain-str
               :shadow.build.resource/resource
               resource))

     ; (println (take 4 (map (fn [[k v]]
     ;                         [k (type v)])
     ;                       (:classpath build-state))))
     ; (println (cp/make-fs-resource (file registry-path) registry-name))

     (println resource)

     (cp/file-add (:classpath build-state)
                  (.getParentFile (.getParentFile (file registry-path)))
                  (file registry-path))

     ; TODO: Add it to build-state
     (-> build-state
         (data/add-source resource)
         (update :build-sources conj resource-id)))))
