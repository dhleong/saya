(ns saya.hooks.ci
  (:require
   [clojure.java.io :refer [make-parents]]
   [clojure.string :as str]))

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
         registry-path "target/ci-src/saya/ci.cljs"]
     (make-parents registry-path)
     (spit
      registry-path
      (str "
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

(test/run-all-tests #\".*-test$\")")))

   ; TODO: Add it to build-state
   build-state))
