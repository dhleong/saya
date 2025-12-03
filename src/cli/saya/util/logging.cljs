(ns saya.util.logging
  (:require
   ["node:console" :refer [Console]]
   ["node:stream" :refer [PassThrough]]
   [applied-science.js-interop :as j]
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [saya.modules.logging.core :refer [log]]))

(def ^:private patched-console-keys
  ["assert"
   "count"
   "countReset"
   "debug"
   "dir"
   "dirxml"
   "error"
   "group"
   "groupCollapsed"
   "groupEnd"
   "info"
   "log"
   "profile"
   "profileEnd"
   "table"
   "time"
   "timeEnd"
   "timeLog"
   "trace"
   "warn"])

(defn- noisy? [s]
  ; Even in a prod build, react whines in some situations about a state
  ; update against an unmounted component, but Reagent does seem to clean
  ; up properly so... just suppress the warning.
  (str/includes? s "unmounted component"))

(defn- fake-stream [prefix]
  (doto (PassThrough.)
    (.on "data" (fn [data]
                  (let [s (if (string? data)
                            data
                            (str data))]
                    (when-not (noisy? s)
                      (log prefix s)))))))

(defn patch
  "Patch various logging methods to avoid messing up the CLI UI"
  []
  ; Stop re-frame loggers, etc. from trashing our cli UI
  (let [nop (fn [& _])]
    (re-frame/set-loggers!
     {:log      (partial nop :info)
      :warn     (partial log :warn)
      :error    (partial log :error)
      :debug    (partial nop :debug)
      :group    (partial nop :info)
      :groupEnd  #()})

    (let [fake-stdout (fake-stream "[stdout]")
          fake-stderr (fake-stream "[stderr]")
          my-console (Console. fake-stdout fake-stderr)]

      ; Catch any direct stdout writes:
      (js/Object.defineProperty
       js/process
       "stdout"
       #js {:get (constantly fake-stdout)
            :enumerable true})

      ; Unclear why we can't just (j/assoc! js/globoal :console my-console)
      ; but for whatever reason, that causes node to barf and exit
      (doseq [prop patched-console-keys]
        (j/assoc! js/console prop (j/get my-console prop))))))
