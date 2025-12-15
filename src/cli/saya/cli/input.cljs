(ns saya.cli.input
  (:require
   ["ink" :as k]
   ["react" :as React]
   [archetype.util :refer [>evt]]
   [saya.cli.keys :refer [->key]]
   [saya.modules.input.core :as input]))

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
   (React/useEffect
    (fn []
      (let [[last-token set-token]
            (swap-vals!
             active-token
             (fn [old]
               (let [new (resolve-token get-token)]
                 (if-not (= old new)
                   new
                   (do
                     (when-not (= :dispatcher new)
                       (>evt [:echo :error "Duplicate input token: " new]))
                     old)))))]

        (fn unmount-use-keys []
          (when-not (= last-token set-token)
            (reset! active-token last-token)))))
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
