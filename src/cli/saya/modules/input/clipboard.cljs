(ns saya.modules.input.clipboard
  (:require
   ["clipboardy" :default clipboardy]
   [clojure.core.match :as m]
   [clojure.string :as str]
   [re-frame.core :refer [reg-fx]]
   [saya.modules.buffers.line :refer [buffer-line]]))

(defn cofx-enabled-integration? [cofx]
  (= :unnamedplus
     (get-in cofx [:db :config :clipboard])))

(defn update-context [ctx cofx]
  (cond-> ctx
    (cofx-enabled-integration? cofx)
    (assoc-in [:registers \"] (let [content (.readSync clipboardy)]
                                (if (str/ends-with? content "\n")
                                  {:lines (->> content
                                               (str/split-lines)
                                               (map buffer-line))}
                                  {:chars content})))))

(reg-fx
 ::write
 (fn [new-content]
   (m/match [new-content]
     [{:chars s}] (.write clipboardy s)
     [{:lines s}] (.write clipboardy
                          (str (str/join "\n" s) "\n")))))
