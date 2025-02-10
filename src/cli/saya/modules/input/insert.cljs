(ns saya.modules.input.insert
  (:require
   [clojure.string :as str]
   [saya.modules.ansi.split :as split]
   [saya.modules.input.helpers :refer [current-buffer-eol-col]]
   [saya.modules.input.normal :refer [to-start-of-line]]))

(defn to-after-end-of-line [{:keys [buffer]}]
  {:buffer (assoc-in buffer [:cursor :col]
                     (current-buffer-eol-col buffer))})

(def movement-keymaps
  {[:ctrl/a] to-start-of-line
   [:ctrl/e] to-after-end-of-line})

; TODO: basic delete keymaps like :delete

(def keymaps
  (merge
   movement-keymaps))

(defn- insert-at-cursor [{:keys [lines cursor] :as buffer} key]
  (let [linenr (:row cursor)
        text (mapcat (comp split/chars-with-ansi :ansi)
                     (nth lines linenr []))
        line' (-> (take (:col cursor) text)
                  (concat [key])
                  (concat (drop (:col cursor) text))
                  (str/join))]
    (-> buffer
        (assoc-in [:lines linenr] [{:ansi line' :plain line'}])
        (update-in [:cursor :col] + (count key)))))

(defn insert-at-buffer [{:keys [bufnr] :as cofx} key]
  (update-in cofx [:db :buffers bufnr] insert-at-cursor key))
