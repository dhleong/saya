(ns saya.modules.input.prompt
  (:require
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
      (assoc-in ctx [:buffer :lines 0]
                (buffer-line (first after)))

      (and (seq before)
           (= :newer direction))
      (assoc-in ctx [:buffer :lines 0]
                (buffer-line (peek before)))

      ; TODO: We should be able to store the original input value,
      ; and restore it here.
      (= :newer direction)
      (assoc-in ctx [:buffer :lines 0] (buffer-line))

      ; Just stay where we are, I guess
      :else ctx)))

(def prompt-keymaps
  {["k"] (comp
          clamp-cursor
          (partial scroll-input-history :older))
   ["j"] (comp
          clamp-cursor
          (partial scroll-input-history :newer))})

; TODO: Need to swap the "current" buffer back before these work:
(def mode-change-keymaps
  ; TODO: any scroll functions should probably revert back to :normal
  {[:ctrl/k] (comp
              (update-cursor :row dec)
              (mode<- :normal))
   [:ctrl/j] (comp
              (update-cursor :row inc)
              (mode<- :normal))})

(def keymaps
  (merge
   normal/keymaps
   prompt-keymaps
   mode-change-keymaps))
