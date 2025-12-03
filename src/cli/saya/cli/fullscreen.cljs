(ns saya.cli.fullscreen
  (:require
   ["ink" :as k]
   ["react" :as React]
   [applied-science.js-interop :as j]
   [archetype.util :refer [<sub >evt]]
   [promesa.core :as p]
   [saya.events :as events]))

(defn use-stdout-dimensions []
  (j/let [^js {:keys [stdout]} (k/useStdout)
          [dimens set-dimens!] (React/useState
                                [(.-columns stdout)
                                 (.-rows stdout)])]

    (React/useEffect
     (fn []
       (let [handle-resize (fn handle-resize []
                             (set-dimens!
                              [(.-columns stdout)
                               (.-rows stdout)]))]
         (.on stdout "resize" handle-resize)

         (fn on-unmount []
           (.off stdout "resize" handle-resize))))

     #js [stdout])

    dimens))

(defn dimens-tracker []
  (let [[width height] (use-stdout-dimensions)]
    (>evt [::events/set-dimens width height])))

(def ^:private ansi-enter-alternate-screen "\u001b[?1049h")
(def ^:private ansi-leave-alternate-screen "\u001b[?1049l")

(defn activate-alternate-screen [& {:keys [on-deactivate stdout]
                                    :or {stdout js/process.stdout}}]
  ; NOTE: Somewhat hacky way to use the alternate screen:
  (let [active? (atom false)
        write (fn write [s]
                (p/create
                 (fn [resolve reject]
                   (.write stdout
                           s
                           (fn [err]
                             (if err
                               (reject err)
                               (resolve)))))))
        activate! (fn activate []
                    (when (compare-and-set! active? false true)
                      (write ansi-enter-alternate-screen)))
        deactivate! (fn deactivate []
                      (when (compare-and-set! active? true false)
                        (p/do!
                         (when on-deactivate
                           (on-deactivate))
                         (write ansi-leave-alternate-screen)
                         (js/process.exit))))]
    (p/do!
     (activate!)
     (js/process.on "SIGINT" deactivate!)
     (js/process.on "beforeExit" deactivate!)
     (js/process.on "exit" deactivate!))))

(defn fullscreen-box [& children]
  (let [[props children] (if (map? (first children))
                           [(first children)
                            (rest children)]
                           [nil
                            children])
        dimens (<sub [:dimens])
        props (merge dimens props)]
    (into [:> k/Box props] children)))
