(ns saya.util.ink
  (:require ["ansi-escapes" :as ansi]
            [applied-science.js-interop :as j]))

(defn update-screen [{:keys [out last-output] :as state} output]
  (when-not (= last-output output)
    (.write out ansi/clearTerminal)
    (.write out output))

  (-> state
      (assoc :last-output output)))

(defn- strip-provided-ansi [output]
  ; Let update-screen work with a clean slate. Since we're
  ; always a "full screen" app, output should *always* start
  ; with clearTerminal
  (subs output (count ansi/clearTerminal)))

(defn stdout []
  (let [out js/process.stdout
        state (atom {:out out})]

    (js/Object.defineProperties
     (j/obj .-write (partial swap! state
                             (fn [state str]
                               (->> str
                                    (strip-provided-ansi)
                                    (update-screen state))))
            .-on (.bind (.-on out) out)
            .-off (.bind (.-off out) out))
     #js {:rows #js {:get #(.-rows out)}
          :columns #js {:get #(.-columns out)}})))
