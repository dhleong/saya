(ns saya.util.ink-testing-utils
  (:require
   ["ink" :as k]
   ["node:events" :refer [EventEmitter]]
   ["strip-ansi" :default strip-ansi]
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [saya.modules.ui.cursor :refer [strip-cursor]]
   [saya.prelude]
   [saya.util.ink :as ink]))

(defn render->string
  ([component] (render->string {} component))
  ([{:keys [width height ansi?]
     :or {width 40
          height 20}}
    component]
   (let [last-frame (atom "")
         events (EventEmitter.)
         stdout (js/Object.defineProperties
                 (j/obj .-write (fn [f]
                                  (swap! last-frame str f))
                        .-on (.bind (.-on events) events)
                        .-off (.bind (.-off events) events))
                 #js {:rows #js {:get (constantly height)}
                      :columns #js {:get (constantly width)}})
         ink-state (atom {:out stdout
                          :cursor-shape? false})
         inst (k/render (r/as-element component)
                        #js {:stdout (ink/stdout {:always-render? true} ink-state stdout)
                             :debug true
                             :extOnCtrlC false
                             :patchConsole false})]
     (doto inst
       (.unmount)
       (.cleanup))
     (cond-> (strip-cursor (:last-output @ink-state))
       (not ansi?) (strip-ansi)))))
