(ns saya.util.ink-testing-utils
  (:require
   ["ink" :as k]
   ["node:events" :refer [EventEmitter]]
   ["strip-ansi" :default strip-ansi]
   [applied-science.js-interop :as j]
   [archetype.util :refer [>evt]]
   [saya.modules.ui.cursor :refer [strip-cursor]]
   [saya.reagent :as reagent]
   [saya.util.ink :as ink]))

(defn- create-fake-stdout [last-frame width height]
  (let [events (EventEmitter.)]
    (js/Object.defineProperties
     (j/obj .-write (fn [f]
                      (swap! last-frame str f))
            .-on (.bind (.-on events) events)
            .-off (.bind (.-off events) events))
     #js {:rows #js {:get (constantly height)}
          :columns #js {:get (constantly width)}})))

(defn- create-fake-stdin []
  (let [events (EventEmitter.)]
    (js/Object.defineProperties
     (j/obj .-isTTY true
            .-setRawMode (fn [_] nil)
            .-setEncoding (fn [_] nil)
            .-resume (fn [] nil)
            .-pause (fn [] nil)
            .-ref (fn [] nil)
            .-unref (fn [] nil)
            .-on (.bind (.-on events) events)
            .-addListener (.bind (.-addListener events) events)
            .-removeListener (.bind (.-removeListener events) events)
            .-off (.bind (.-off events) events)
            .-emit (.bind (.-emit events) events))
     #js {})))

(defn render->string
  ([component] (render->string {} component))
  ([{:keys [width height ansi? position-cursor?]
     :or {width 40
          height 20
          position-cursor? true}}
    component]
   (>evt [:saya.events/set-dimens width height])
   (let [last-frame (atom "")
         stdout (create-fake-stdout last-frame width height)
         stdin (create-fake-stdin)
         ink-state (atom {:out stdout
                          :cursor-shape? false
                          :position-cursor? position-cursor?})
         root (reagent/as-root component)
         inst (k/render root
                        #js {:stdout (ink/stdout
                                      {:always-render? true}
                                      ink-state stdout)
                             :stdin stdin
                             :stderr (create-fake-stdout (atom "") width height)
                             :debug true
                             :extOnCtrlC false
                             :patchConsole false})

         _ (.rerender inst root)

         rendered (cond-> (strip-cursor (:last-output @ink-state))
                    (not ansi?) (strip-ansi))]
     (doto inst
       (.unmount)
       (.cleanup))
     rendered)))
