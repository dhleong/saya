(ns saya.modules.layout.components
  (:require
   ["ink" :as k]
   ["node:fs/promises" :as fs]
   ["node:path" :as path]
   ["react" :as React]
   [archetype.util :refer [<sub >evt]]
   [clojure.string :as str]
   [promesa.core :as p]
   [saya.modules.echo.core :refer [echo]]
   [saya.modules.logging.core :refer [log]]
   [saya.modules.window.view :refer [window-view]]))

(defn- build-box [{:keys [background-color flex-direction height width]}]
  [:> k/Box {:flex-direction flex-direction
             :flex-grow (when-not (number? height)
                          1)
             :background-color (when (or (string? background-color)
                                         (keyword? background-color))
                                 background-color)
             :height (when (number? height)
                       height)
             :width (if (or (number? width)
                            (keyword? width))
                      width
                      :100%)
             :overflow :hidden
             :flex 1}])

(defn horizontal [opts & children]
  (into (build-box (assoc opts :flex-direction :row))
        children))

(defn vertical [opts & children]
  (into (build-box (assoc opts :flex-direction :column))
        children))

(defn edit-file-view [{:keys [key script-file]} filename]
  (log "render edit-file-view " filename " into " key)
  (React/useEffect
   (fn []
      ; HACKS: Load file into buffer with key
     (log "read " filename " into " key)
     ; FIXME: load relative to script file
     (-> (p/let [script-dir (or (when script-file
                                  (path/dirname script-file))
                                "./")
                 contents (fs/readFile (path/join script-dir filename)
                                       #js {:encoding "utf-8"})]
           (log "read from " filename " " (count contents))
           (>evt [:saya.modules.layout.events/set-keyed-buffer-contents
                  {:key key
                   :string contents}]))
         (p/catch (fn [e]
                    (echo :exception "Failed to load " filename ": " e))))
     js/undefined)
   #js [key filename])

  (let [{:keys [winnr]} (<sub [:saya.modules.layout.subs/key key])]
    [:> k/Box {:flex-direction :column
               :flex-grow 1
               :width :100%
               :flex 1}
     [window-view winnr]
     [:> k/Text filename "#" winnr]
     ; TODO:
     #_[:> k/Text "TODO: file@" (str filename)
        winnr]]))

(defn edit-string-view [{:keys [key]} content]
  ; TODO: Store content in DB state for window
  ; (React/useEffect
  ;   (fn []
  ;     (>evt [::events/update])
  ;     js/undefined)
  ;   #js [content])
  [:> k/Box {:flex-direction :row
             :flex-grow 1
             :width :100%
             :flex 1}
   [:> k/Text "TODO: " (if (string? content)
                         content
                         (str/join "\n" content))]])

(defn edit-ref-view [params the-ref]
  (let [v (try @the-ref
               (catch :default e
                 (str "ERROR: Unable to deref reference: " e)))]
    [edit-string-view params v]))
