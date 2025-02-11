(ns saya.modules.input.normal
  (:require
   [saya.modules.input.helpers :refer [adjust-cursor-to-scroll
                                       adjust-scroll-to-cursor clamp-cursor
                                       clamp-scroll
                                       current-buffer-line-last-col
                                       last-buffer-row]]
   [saya.modules.logging.core :refer [log]]
   [saya.modules.buffers.util :as buffers]))

; ======= Movement keymaps =================================

(defn update-cursor [col-or-row f]
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn cursor-updator [ctx]
     (update-in ctx [:buffer :cursor col-or-row] f))))

(def scroll-to-bottom
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn to-last-line [{:keys [buffer] :as ctx}]
     (assoc-in ctx [:buffer :cursor :row]
               (last-buffer-row buffer)))))

(defn to-start-of-line [{:keys [buffer]}]
  {:buffer (assoc-in buffer [:cursor :col] 0)})

(defn to-end-of-line [{:keys [buffer]}]
  {:buffer (assoc-in buffer [:cursor :col]
                     (current-buffer-line-last-col buffer))})

(def movement-keymaps
  {["0"] to-start-of-line
   ["$"] to-end-of-line

   ["g" "g"] (comp
              clamp-scroll
              adjust-scroll-to-cursor
              (fn to-first-line [ctx]
                (assoc-in ctx [:buffer :cursor] {:col 0
                                                 :row 0})))

   ["G"] scroll-to-bottom

   ; Single char movement
   ["k"] (update-cursor :row dec)
   ["j"] (update-cursor :row inc)
   ["h"] (update-cursor :col dec)
   ["l"] (update-cursor :col inc)})

; ======= Operator keymaps =================================

(defn delete-operator [context motion-range]
  ; TODO:
  (log "DELETE"))

(defn- enqueue-operator [operator]
  (fn operator-keymap [{:keys [buffer] :as context}]
    (if (buffers/readonly? buffer)
      {:error "Read-only buffer"}

      (assoc context
             :mode :operator-pending
             :pending-operator operator))))

(def operator-keymaps
  {["d"] (enqueue-operator delete-operator)})

; ======= Scroll keymaps ===================================

(defn- update-scroll [f compute-amount]
  (comp
   adjust-scroll-to-cursor
   adjust-cursor-to-scroll
   clamp-cursor
   clamp-scroll
   (fn scroll-updater [{:keys [buffer] :as ctx}]
     (update-in ctx [:window :anchor-row]
                (fnil f (last-buffer-row buffer))
                (compute-amount ctx)))))

(defn- window-rows [{:keys [window]}]
  (dec (:height window)))

(def scroll-keymaps
  {[:ctrl/y] (update-scroll - (constantly 1))
   [:ctrl/e] (update-scroll + (constantly 1))

   [:ctrl/b] (update-scroll - window-rows)
   [:ctrl/f] (update-scroll + window-rows)})

(def keymaps
  (merge
   movement-keymaps
   operator-keymaps
   scroll-keymaps))

#_{:clj-kondo/ignore [:unresolved-namespace]}
(comment
  (get-in @re-frame.db/app-db [:buffers 0 :cursor])
  (get-in @re-frame.db/app-db [:windows 0])

  (let [context (saya.modules.input.keymaps/build-context
                 {:bufnr 0 :winnr 0
                  :db @re-frame.db/app-db})]
    (-> context

        ((comp
           ; clamp-scroll
          adjust-scroll-to-cursor
          (fn to-first-line [ctx]
            (assoc-in ctx [:buffer :cursor] {:col 0
                                             :row 0}))))

        ((juxt
          #(assoc {} :cursor (get-in % [:buffer :cursor]))
          #(select-keys % [:window]))))))
