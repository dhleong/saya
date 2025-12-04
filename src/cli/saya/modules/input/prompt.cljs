(ns saya.modules.input.prompt
  (:require
   [medley.core :refer [map-vals]]
   [saya.modules.buffers.line :refer [buffer-line]]
   [saya.modules.input.helpers :refer [clamp-cursor update-cursor]]
   [saya.modules.input.normal :as normal :refer [mode<-]]
   [saya.util.coll :refer [split-when]]))

(defn scroll-input-history [direction {:keys [buffer histories] :as ctx}]
  (let [current (str (first (:lines buffer)))
        [before _ after] (split-when
                          (partial = current)
                          ::not-found
                          (get histories (:id buffer)))]
    (cond
      ; Initial state
      (and (seq after)
           (= :older direction))
      (-> ctx
          (assoc-in [:buffer :lines 0]
                    (buffer-line (first after)))
          (assoc-in [:buffer ::original-input] (:lines buffer)))

      (and (seq before)
           (= :newer direction))
      (assoc-in ctx [:buffer :lines 0]
                (buffer-line (peek before)))

      (and (= :newer direction)
           (::original-input buffer))
      (-> ctx
          (assoc-in [:buffer :lines] (::original-input buffer))
          (update :buffer dissoc ::original-input))

      ; Just stay where we are, I guess
      :else ctx)))

(def prompt-keymaps
  {["k"] (comp
          clamp-cursor
          (partial scroll-input-history :older))
   ["j"] (comp
          clamp-cursor
          (partial scroll-input-history :newer))})

(defn- in-normal-mode [f]
  (comp
   (normal/with-named-buffer
     :normal-buffer
     f)
   (mode<- :normal)))

(def mode-change-keymaps
  (merge
   {[:ctrl/k] (in-normal-mode
               (update-cursor :row dec))
    [:ctrl/j] (in-normal-mode
               (update-cursor :row inc))}

    ; Any scroll functions should probably revert back to :normal
   (map-vals
    in-normal-mode
    (select-keys
     normal/movement-keymaps
     [["g" "g"]
      ["G"]]))

   (map-vals
    in-normal-mode
    normal/scroll-keymaps)))

(def keymaps
  (merge
   normal/keymaps
   prompt-keymaps
   mode-change-keymaps))
