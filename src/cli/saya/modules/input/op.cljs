(ns saya.modules.input.op
  (:require
   [saya.modules.input.helpers :refer [adjust-scroll-to-cursor clamp-cursor
                                       clamp-scroll]]
   [saya.modules.input.motions.word :refer [end-of-word-movement
                                            small-word-boundary? word-movement]]
   [saya.modules.input.normal :as normal]
   [saya.modules.input.shared :refer [to-end-of-line to-start-of-line]]))

(defn- range-getter->motion [get-range]
  (fn range-getter-motion [{:keys [pending-operator] :as context}]
    (let [motion-range (get-range context)
          ; TODO: There's eg o_v for turning a normally line-wise
          ; motion into a character-wise one
          context (dissoc context ::linewise?)]
      (if-not (= (:start motion-range) (:end motion-range))
        (-> context
            (pending-operator motion-range)
            (assoc :pending-operator nil)
            (as-> context''
              ; In case the operator returned an error, for example:
              (merge context context''))
            (clamp-cursor)
            (adjust-scroll-to-cursor)
            (clamp-scroll))

        context))))

; ======= Movement -> motions ==============================

(defn- extract-flags [f]
  (meta f))

(defn movement->motion [f]
  (range-getter->motion
   (fn get-movement-range [context]
     (let [context' (f context)
           start (get-in context [:buffer :cursor])
           end (get-in context' [:buffer :cursor])]
       (merge
        (extract-flags f)
        {:start start
         :end end
         :linewise? (or (::linewise? context)
                        (not= (:row start) (:row end)))})))))

(def movement-keymaps
  (->> normal/movement-keymaps
       (map (fn [[k v]]
              [k (movement->motion v)]))
       (into {})))

; ======= Word text objects ================================

; FIXME: The :start for these ought to not move when starting on a
; boundary...

(def inner-word
  (range-getter->motion
   (fn [context]
     {:start (-> context
                 ((word-movement dec small-word-boundary?))
                 (get-in [:buffer :cursor]))
      :end (-> context
               ((end-of-word-movement inc small-word-boundary?))
               (get-in [:buffer :cursor]))
      :inclusive? true})))

(def outer-word
  (range-getter->motion
   (fn [context]
     {:start (-> context
                 ((word-movement dec small-word-boundary?))
                 (get-in [:buffer :cursor]))
      :end (-> context
               ((word-movement inc small-word-boundary?))
               (get-in [:buffer :cursor]))})))

(def word-object-keymaps
  {["i" "w"] inner-word
   ["a" "w"] outer-word})

; ======= Public interface =================================

(def keymaps
  (merge
   movement-keymaps
   word-object-keymaps))

(def full-line-keymap
  {[:full-line] (comp
                 (movement->motion #'to-end-of-line)
                 (fn [ctx]
                   (assoc ctx ::linewise? true))
                 to-start-of-line)})
