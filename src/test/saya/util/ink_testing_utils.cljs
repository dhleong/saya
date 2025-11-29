(ns saya.util.ink-testing-utils
  (:require
   ["ink" :as k]
   ["node:events" :refer [EventEmitter]]
   ["strip-ansi" :default strip-ansi]
   [applied-science.js-interop :as j]
   [archetype.util :refer [>evt]]
   [saya.modules.ui.cursor :refer [strip-cursor]]
   [saya.reagent :as reagent]
   [saya.util.ink :as ink]
   [clojure.string :as str]
   [re-frame.core :as rf]))

(defn- create-fake-stdout [last-frame width height]
  (let [events (EventEmitter.)]
    (js/Object.defineProperties
     (j/obj .-write (fn [f]
                      (swap! last-frame str f))
            .-on (.bind (.-on events) events)
            .-off (.bind (.-off events) events))
     #js {:rows #js {:get (constantly height)}
          :columns #js {:get (constantly width)}})))

(defn- create-fake-stdin [data]
  (let [events (EventEmitter.)]
    (add-watch data :stdin-pipe
               (fn [_ _ old new]
                 (when (and new (nil? old))
                   (.emit events "readable")
                   (.emit events "data" new))))
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
           .-emit (.bind (.-emit events) events)
           .-read (fn []
                    (let [[old _new] (reset-vals! data nil)]
                      old)))))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defprotocol IRenderer
  (render-string [this opts])
  (resize [this width height])
  (feed-stdin [this text])
  (cleanup [this]))

(deftype Renderer [opts stdin-state ink-state ^js ink root]
  IRenderer
  (render-string [_this new-opts]
    (.rerender ink root)

    (let [{:keys [ansi?]} (merge opts new-opts)]
      (when-let [output (:last-output @ink-state)]
        (cond-> (strip-cursor output)
          (not ansi?) (strip-ansi)))))

  (resize [_this width height]
    (>evt [:saya.events/set-dimens width height]))

  (feed-stdin [_this text]
    (reset! stdin-state text))

  (cleanup [_this]
    (rf/clear-subscription-cache!)
    (swap! ink-state dissoc :last-output)
    (doto ink
      (.unmount)
      (.cleanup))))

(defn create-renderer
  ([component] (create-renderer {} component))
  #_{:clj-kondo/ignore [:unused-binding]}
  ([{:keys [width height ansi? position-cursor?]
     :or {width 40
          height 20
          position-cursor? true}
     :as opts}
    component]
   (>evt [:saya.events/set-dimens width height])
   (let [last-frame (atom "")
         stdin-state (atom nil)
         stdout (create-fake-stdout last-frame width height)
         stdin (create-fake-stdin stdin-state)
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
                             :exitOnCtrlC false
                             :patchConsole false})]
     (->Renderer opts stdin-state ink-state inst root))))

(defn render->string
  ([component] (render->string {} component))
  ([opts component]
   (let [renderer (if (satisfies? IRenderer component)
                    component
                    (create-renderer opts component))]
     (when (:last-output @(.-ink-state renderer))
       (rf/clear-subscription-cache!))
     (render-string renderer opts))))

(defn render->vec
  ([component] (render->vec {} component))
  ([opts component]
   (-> (render->string opts component)
       (str/split-lines))))

(defn input! [renderer text]
  (feed-stdin renderer text))
