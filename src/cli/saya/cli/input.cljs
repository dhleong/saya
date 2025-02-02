(ns saya.cli.input
  (:require
   ["ink" :as k]
   ["react" :as React]
   [archetype.util :refer [>evt]]
   [saya.cli.keys :refer [->key]]
   [saya.modules.input.core :as input]
   [saya.modules.logging.core :refer [log]]))

(defonce ^:private active-token (atom nil))

(defn- resolve-token [get-token]
  (if (keyword? get-token)
    get-token
    (get-token)))

(defn use-keys
  ([on-key]
   (let [token-ref (React/useRef [(js/Date.now) (js/Math.random)])]
     (use-keys #(.-current token-ref) on-key)))
  ([get-token on-key]
   (log "mount" (resolve-token get-token))

   (React/useEffect
    (fn []
      (let [[last-token _] (reset-vals!
                            active-token
                            (resolve-token get-token))]

        (fn unmount-use-keys []
          (reset! active-token last-token))))
    #js [])

   (k/useInput
    (fn input-dispatcher [input k]
      (when (= @active-token (resolve-token get-token))
        (let [the-key (->key input k)]
          (on-key the-key)))))

   ; Convenience for functional components that don't render:
   nil))

(defn dispatcher []
  (use-keys :dispatcher #(>evt [::input/on-key %])))
