(ns saya.util.string
  (:require
   [clojure.string :as str]))

(def ^:private custom-slug-chars
  {":" "__"
   "/" "_SLASH_"})

(defn slugify [s]
  (-> s
      (str/replace #"[^a-zA-Z0-9_-]"
                   (fn [v]
                     (get custom-slug-chars v "_")))))

(defn trim-suffix [s suffix]
  (cond-> s
    (str/ends-with? s suffix) (subs 0 (- (count s)
                                         (count suffix)))))

