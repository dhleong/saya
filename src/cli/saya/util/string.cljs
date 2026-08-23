(ns saya.util.string
  (:require
   [clojure.string :as str]))

(defn trim-suffix [s suffix]
  (cond-> s
    (str/ends-with? s suffix) (subs 0 (- (count s)
                                         (count suffix)))))

