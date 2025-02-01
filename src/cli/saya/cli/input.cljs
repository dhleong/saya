(ns saya.cli.input
  (:require
   ["ink" :as k]
   ["react" :as React]
   [archetype.util :refer [>evt]]
   [saya.cli.keys :refer [->key]]
   [saya.modules.input.core :as input]))

(defonce ^:private active-token (atom nil))

(defn use-keys [on-key]
  (let [token-ref (React/useRef [(js/Date.now) (js/Math.random)])]

    (React/useEffect
     (fn []
       (let [[last-token _] (reset-vals!
                             active-token
                             token-ref.current)]

         (fn unmount-use-keys []
           (reset! active-token last-token))))
     #js [])

    (k/useInput
     (fn input-dispatcher [input k]
       (when (= @active-token token-ref.current)
         (let [the-key (->key input k)]
           (on-key the-key))))))

  ; Convenience for functional components that don't render:
  nil)

(defn dispatcher []
  (use-keys #(>evt [::input/on-key %])))
