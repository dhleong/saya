(ns saya.modules.input.op
  (:require
   [clojure.string :as str]
   [saya.modules.input.helpers :refer [adjust-scroll-to-cursor clamp-cursor
                                       clamp-scroll]]
   [saya.modules.input.motions.find :refer [perform-find-ch perform-until-ch]]
   [saya.modules.input.motions.word :refer [small-word-boundary?]]
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

(defn- ->cursor [ctx]
  (get-in ctx [:buffer :cursor]))

(def inner-word
  (let [->start #(perform-until-ch % dec small-word-boundary?)
        ->end #(perform-until-ch % inc small-word-boundary?)]
    (range-getter->motion
     (fn [context]
       {:start (-> context
                   (->start)
                   (->cursor))
        :end (-> context
                 (->end)
                 (->cursor))
        :inclusive? true}))))

; FIXME: The :start for this ought to not move when starting on a
; boundary...

(defn- consume-whitespace [ctx increment]
  (-> ctx
      ; Find whitespace
      (perform-find-ch increment str/blank?)
      ; Consume it
      (perform-until-ch increment (complement str/blank?))))

(def outer-word
  (range-getter->motion
   (fn [ctx]
     (let [inner-end (->cursor (perform-until-ch ctx inc small-word-boundary?))
           word-end (->cursor (consume-whitespace ctx inc))
           ->start (if (= inner-end word-end)
                     ; If there was trailing whitespace, word-end should be >
                     ; inner-end. Here, it's = so there must not be---so, let's
                     ; consume leading whitespace instead
                     #(consume-whitespace % dec)
                     #(perform-until-ch % dec small-word-boundary?))]
       {:start (-> ctx
                   (->start)
                   (->cursor))
        :end word-end
        :inclusive? true}))))

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
