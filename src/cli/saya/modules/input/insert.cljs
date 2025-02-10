(ns saya.modules.input.insert
  (:require
   [saya.cli.text-input.helpers :refer [dec-to-zero split-text-by-state]]
   [saya.modules.input.helpers :refer [current-buffer-eol-col]]
   [saya.modules.input.normal :refer [to-start-of-line]]))

(defn- line->string [line]
  (->> line
       (mapcat :ansi)
       (apply str)))

(defn- update-cursor-line-string [{:keys [buffer] :as context} f]
  (let [{linenr :row} (:cursor buffer)]
    (-> context
        (update-in [:buffer :lines linenr]
                   (comp
                    (fn [line'] [{:ansi line' :plain line'}])
                    f
                    (fnil line->string []))))))

(defn to-after-end-of-line [{:keys [buffer]}]
  {:buffer (assoc-in buffer [:cursor :col]
                     (current-buffer-eol-col buffer))})

(def movement-keymaps
  {[:ctrl/a] to-start-of-line
   [:ctrl/e] to-after-end-of-line})

(defn backspace [{:keys [buffer] :as context}]
  (-> context
      (update-cursor-line-string
       (fn [line]
         (let [[before after] (split-text-by-state buffer line)]
           (str
            (when (seq before)
              (subs before 0 (dec (count before))))
            after))))
      (update-in [:buffer :cursor :col] dec-to-zero)))

(def basic-editing-keymaps
  {[:delete] backspace})

(def keymaps
  (merge
   basic-editing-keymaps
   movement-keymaps))

(defn insert-at-cursor [{:keys [buffer] :as context} key]
  (-> context
      (update-cursor-line-string
       (fn [line]
         (let [[before after] (split-text-by-state buffer line)]
           (str before key after))))
      (update-in [:buffer :cursor :col] + (count key))))
