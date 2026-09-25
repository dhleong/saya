(ns saya.modules.layout.components
  (:require
   ["ink" :as k]
   ["node:fs/promises" :as fs]
   ["node:path" :as path]
   ["react" :as React]
   [archetype.util :refer [<sub >evt]]
   [promesa.core :as p]
   [saya.modules.echo.core :refer [echo]]
   [saya.modules.logging.core :refer [log]]))

(def window-view
  (delay (resolve 'saya.modules.window.view/window-view)))

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

(defn- container [& children]
  (into [:> k/Box {:flex-direction :column
                   :flex-grow 1
                   :width :100%
                   :flex 1}]
        children))

(defn- keyed-window-view [k]
  (let [{:keys [winnr]} (<sub [:saya.modules.layout.subs/key k])]
    [@window-view winnr]))

(defn edit-file-view [{:keys [key script-file]} filename]
  (log "render edit-file-view " filename " into " key)
  (React/useEffect
   (fn []
     ; NOTE: Is it hacky to do this as a use-effect like this?
     ; ... Probably
     (-> (p/let [script-dir (or (when script-file
                                  (path/dirname script-file))
                                "./")
                 contents (fs/readFile (path/join script-dir filename)
                                       #js {:encoding "utf-8"})]
           (>evt [:saya.modules.layout.events/set-keyed-buffer-contents
                  {:key key
                   :string contents}]))
         (p/catch (fn [e]
                    (echo :exception "Failed to load " filename ": " e))))
     js/undefined)
   #js [key filename])

  [container
   [keyed-window-view key]
   ; TODO: Better, more consistent statuslines
   [:> k/Text {:dim-color true} filename]])

(defn edit-string-view [{:keys [key]} content]
  ; TODO: Store content in DB state for window
  (React/useEffect
   (fn []
     (>evt [:saya.modules.layout.events/set-keyed-buffer-contents
            {:key key
             :content content}])
     js/undefined)
   #js [content])
  [container
   [keyed-window-view key]])

(defn edit-ref-view [params the-ref]
  (let [v (try @the-ref
               (catch :default e
                 (str "ERROR: Unable to deref reference: " e)))]
    [edit-string-view params v]))
