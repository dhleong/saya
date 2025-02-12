(ns saya.modules.input.normal
  (:require
   [clojure.core.match :refer [match]]
   [saya.modules.buffers.util :as buffers]
   [saya.modules.input.helpers :refer [adjust-cursor-to-scroll
                                       adjust-scroll-to-cursor clamp-cursor
                                       clamp-scroll last-buffer-row
                                       update-cursor]]
   [saya.modules.input.insert :refer [line->string update-buffer-line-string]]
   [saya.modules.input.shared :refer [to-end-of-line to-start-of-line]]))

; ======= Movement keymaps =================================

(def scroll-to-bottom
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn to-last-line [{:keys [buffer] :as ctx}]
     (assoc-in ctx [:buffer :cursor :row]
               (last-buffer-row buffer)))))

(defn word-movement [increment ch-pred]
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn word-mover [{:keys [buffer] :as ctx}]
     (loop [cursor (:cursor buffer)
            found-whitespace? false]
       ; NOTE: This is not quite right, esp for backwards motion
       (match [(buffers/char-at buffer cursor) found-whitespace?]
         ; Can't move further; stop
         [nil _] (assoc-in ctx [:buffer :cursor] cursor)

         [" " _] (recur (buffers/update-cursor buffer cursor increment)
                        true)

         ; Found the target after the whitespace break
         [(_ :guard ch-pred) true] (assoc-in ctx [:buffer :cursor] cursor)

         :else (recur (buffers/update-cursor buffer cursor increment)
                      found-whitespace?))))))

(defn small-word-boundary? [ch]
  (re-matches #"[a-zA-Z0-9]" ch))

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
   ["l"] (update-cursor :col inc)

   ; Word movement
   ["w"] (word-movement inc small-word-boundary?)
   ["b"] (word-movement dec small-word-boundary?)})

; ======= Operator keymaps =================================

(defn- align-start-end [start end]
  [(min start end)
   (max start end)])

(defn- delete-lines [{:keys [lines] :as buffer} start end]
  (let [[start end] (align-start-end start end)
        yanked (subvec lines start (inc end))]
    (-> buffer
        (assoc :lines (into (subvec lines 0 start)
                            (subvec lines (inc end))))
        (assoc-in [:cursor :row] start)
        (assoc :yanked {:lines yanked}))))

(defn- delete-chars [buffer linenr start end]
  (let [[start end] (align-start-end start end)]
    ; TODO: It'd be nice not to have to convert the line to a string multiple times...
    (-> buffer
        (update-buffer-line-string
         linenr
         (fn [line]
           (str (subs line 0 start)
                (subs line end))))
        (assoc-in [:cursor :col] start)
        (assoc :yanked {:chars (subs (line->string (nth (:lines buffer) linenr))
                                     start end)}))))

(defn delete-operator [context {:keys [start end linewise?]}]
  (cond
    ; Line-wise delete
    linewise?
    (update context :buffer delete-lines (:row start) (:row end))

    ; Char-wise delete within a line
    (= (:row start) (:row end))
    (update context :buffer delete-chars (:row start) (:col start) (:col end))

    :else
    {:error "TODO: support char-wise cross-line deletes"}))

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
