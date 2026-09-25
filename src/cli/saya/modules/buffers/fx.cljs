(ns saya.modules.buffers.fx
  (:require
   ["node:fs/promises" :as fs]
   ["node:path" :as path]
   [clojure.string :as str]
   [promesa.core :as p]
   [re-frame.core :refer [reg-fx]]
   [saya.modules.echo.core :refer [echo]]))

(reg-fx
 ::write
 (fn [{:keys [file-path lines]}]
   (-> (p/let [s (->> lines
                      (map str)
                      (str/join "\n"))]
         (fs/writeFile file-path s)
         (echo (str \" (path/basename file-path) \"
                    " "
                    (count lines) "L, "
                    (count s) "B written")))
       (p/catch (fn [e]
                  (echo :exception e))))))
