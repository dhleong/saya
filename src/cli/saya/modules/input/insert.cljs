(ns saya.modules.input.insert
  (:require
   [saya.cli.text-input.helpers :refer [dec-to-zero split-text-by-state]]
   [saya.modules.buffers.line :refer [buffer-line]]
   [saya.modules.input.helpers :refer [update-cursor]]
   [saya.modules.input.shared :refer [to-end-of-line to-start-of-line]]))

(defn line->string [line]
  (str line))

(defn- string->line [s]
  (buffer-line s))

(defn update-buffer-line-string [buffer linenr f]
  (-> buffer
      (update-in [:lines linenr]
                 (comp
                  string->line
                  f
                  (fnil line->string (buffer-line))))))

(defn- update-cursor-line-string [{:keys [buffer] :as context} f]
  (let [{linenr :row} (:cursor buffer)]
    (update context :buffer update-buffer-line-string linenr f)))

(def movement-keymaps
  {[:ctrl/a] to-start-of-line
   [:ctrl/e] to-end-of-line

   [:left] (update-cursor :col dec)
   [:right] (update-cursor :col inc)})

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
