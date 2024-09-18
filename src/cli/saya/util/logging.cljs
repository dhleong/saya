(ns saya.util.logging
  (:require
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [saya.modules.logging.core :refer [log]]))

; ======= stdout patching =================================

(defn patch
  "Patch various logging methods to avoid messing up the CLI UI"
  []
  ; stop re-frame loggers from trashing our cli UI
  (let [nop (fn [& _])
        console-error (atom js/console.error)
        safe-error (fn safe-error [& args]
                     (when-not (and (string? (first args))
                                    (str/includes?
                                     (first args)
                                     "unmounted component"))
                       (apply @console-error args)))]
    (re-frame/set-loggers!
     {:log      (partial nop :info)
      :warn     (partial nop :warn)
      :error    (partial log :error)
      :debug    (partial nop :debug)
      :group    (partial nop :info)
      :groupEnd  #()})

    ; Even in a prod build, react whines in some situations about a state
    ; update against an unmounted component, but Reagent does seem to clean
    ; up properly so... just suppress the warning.
    (js/Object.defineProperties
     js/console
     #js {:error #js {:get (constantly safe-error)
                      :enumerable true
                      :set (fn [replacement]
                             (when replacement
                               (reset! console-error replacement)))}})))
