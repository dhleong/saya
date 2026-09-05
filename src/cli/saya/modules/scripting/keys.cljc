(ns saya.modules.scripting.keys
  (:require
   [clojure.string :as str]))

(defn ->keys
  "Take a user-scripting keys definition (either a string sequence
   or a vector containing string sequences) and canonicalize it into
   a vector"
  [v]
  (cond
    (string? v) (str/split v "")
    (vector? v) (vec (mapcat ->keys v))
    (keyword? v) [v]
    :else (throw (ex-info (str "Invalid keys value: `" v "`")
                          {:v v}))))

