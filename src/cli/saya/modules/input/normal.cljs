(ns saya.modules.input.normal
  (:require
   [saya.cli.text-input.helpers :refer [dec-to-zero]]
   [saya.modules.buffers.line :refer [wrapped-lines]]
   [saya.modules.buffers.util :as buffers]
   [saya.modules.input.helpers :refer [adjust-cursor-to-scroll
                                       adjust-scroll-to-cursor clamp-cursor
                                       clamp-scroll
                                       current-buffer-line-last-col
                                       last-buffer-row update-cursor]]
   [saya.modules.input.insert :refer [line->string update-buffer-line-string]]
   [saya.modules.input.motions.word :refer [big-word-boundary?
                                            end-of-word-movement
                                            small-word-boundary? word-movement]]
   [saya.modules.input.shared :refer [to-end-of-line to-start-of-line]]
   [saya.modules.search.core :as search]))

; ======= Mode-change keymaps ==============================

(defn mode<- [new-mode]
  (fn [ctx]
    (assoc ctx :mode new-mode)))

(defn- with-editable [f]
  (letfn [(swap-keys [v a b]
            (-> v
                (assoc a (b v))
                (assoc b (a v))))]
    (fn [ctx]
      (cond-> ctx
        (:editable ctx)
        (swap-keys :buffer :editable)

        :always
        (f)

        ; Swap back
        (:editable ctx)
        (swap-keys :buffer :editable)))))

(def ^:private mode-change-keymaps
  {["i"] (mode<- :insert)
   ["I"] (comp
          (mode<- :insert)
          (with-editable to-start-of-line))

   ["a"] (comp
          (with-editable
            (fn [{:keys [buffer] :as ctx}]
              (cond-> ctx
                (> (current-buffer-line-last-col buffer) 0)
                (update-in [:buffer :cursor :col] inc))))
          (mode<- :insert))
   ["A"] (comp
          (with-editable to-end-of-line)
          (mode<- :insert))})

; ======= Movement keymaps =================================

(def scroll-to-bottom
  (comp
   clamp-scroll
   adjust-scroll-to-cursor
   clamp-cursor
   (fn to-last-line [{:keys [buffer] :as ctx}]
     (assoc-in ctx [:buffer :cursor :row]
               (last-buffer-row buffer)))))

(def scroll-to-top
  (comp
   adjust-scroll-to-cursor
   (fn to-first-line [ctx]
     (assoc-in ctx [:buffer :cursor] {:col 0
                                      :row 0}))))

(defn next-search-result [get-direction fallback-direction]
  (comp
   adjust-scroll-to-cursor
   (fn next-search-result [{:keys [buffer search] :as ctx}]
     (if-some [query (:query search)]
       (if-some [results (seq
                          (search/in-buffer
                           buffer
                           (get-direction (:direction search fallback-direction))
                           query))]
         (assoc-in ctx [:buffer :cursor] (:at (first results)))
         {:error (str "Pattern not found: " query)})
       {:error "No previous search query"}))))

(def movement-keymaps
  {["0"] #'to-start-of-line
   ["$"] #'to-end-of-line

   ["g" "g"] #'scroll-to-top
   ["G"] #'scroll-to-bottom

   ; Single char movement
   ["k"] (update-cursor :row dec)
   ["j"] (update-cursor :row inc)
   ["h"] (update-cursor :col dec)
   ["l"] (update-cursor :col inc)

   ; Word movement
   ["w"] (word-movement inc small-word-boundary?)
   ["W"] (word-movement inc big-word-boundary?)
   ["b"] (word-movement dec small-word-boundary?)
   ["B"] (word-movement dec big-word-boundary?)

   ; End-of-word movement
   ["e"] (end-of-word-movement inc small-word-boundary?)
   ["E"] (end-of-word-movement inc big-word-boundary?)
   ["g" "e"] (end-of-word-movement dec small-word-boundary?)
   ["g" "E"] (end-of-word-movement dec big-word-boundary?)

   ; Search
   ["n"] (next-search-result identity :newer)
   ["N"] (next-search-result (fn [direction]
                               (case direction
                                 :newer :older
                                 :older :newer))
                             :older)})

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

(defn- delete-chars [buffer {:keys [inclusive?]} linenr start end]
  (let [[start end] (align-start-end start end)
        end (cond-> end
              inclusive? (inc))]
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

(defn delete-operator {:char "d"} [context {:keys [start end linewise?] :as flags}]
  (cond
    ; Line-wise delete
    linewise?
    (update context :buffer delete-lines (:row start) (:row end))

    ; Char-wise delete within a line
    (= (:row start) (:row end))
    (update context :buffer delete-chars flags (:row start) (:col start) (:col end))

    :else
    {:error "TODO: support char-wise cross-line deletes"}))

(defn- enqueue-operator [operator]
  (fn operator-keymap [{:keys [buffer] :as context}]
    (if (and (buffers/readonly? buffer)
             (nil? (:editable context)))
      {:error "Read-only buffer"}

      (assoc context
             :mode :operator-pending
             :pending-operator operator))))

(def operator-keymaps
  {["d"] (enqueue-operator #'delete-operator)})

; ======= Scroll keymaps ===================================

(defn update-scroll [f compute-amount]
  (comp
   ; adjust-scroll-to-cursor
   adjust-cursor-to-scroll
   ; clamp-cursor
   ; clamp-scroll
   (fn scroll-updater [{:keys [buffer window] :as ctx}]
     (loop [anchor-row (or (:anchor-row window)
                           (last-buffer-row buffer))
            anchor-offset (or (:anchor-offset window) 0)
            scroll-to-consume (compute-amount ctx)]
       (let [available-lines (count
                              (wrapped-lines
                               (get-in buffer [:lines anchor-row])
                               (:width window)))
             max-anchor-offset (dec-to-zero available-lines)
             available-lines (- available-lines
                                anchor-offset)
             next-anchor-offset (f anchor-offset
                                   (* -1 scroll-to-consume))]
         (cond
           (and (> available-lines 0)
                (<= 0 next-anchor-offset max-anchor-offset)
                (<= scroll-to-consume available-lines))
           (update ctx :window assoc
                   :anchor-row anchor-row
                   :anchor-offset next-anchor-offset)

           (or (= 0 scroll-to-consume)
               (let [next-row (f anchor-row 1)]
                 (or (> next-row (last-buffer-row buffer))
                     (< next-row 0))))
           (update ctx :window assoc
                   :anchor-row anchor-row
                   :anchor-offset anchor-offset)

           :else
           (recur
            (f anchor-row 1)
            ; TODO: Actually if scrolling forward this probably should be
            ; (dec full-available-lines), I think
            0
            (- scroll-to-consume available-lines))))))))

(defn- window-rows [{:keys [window]}]
  ; Actually a page is 2 less than the window height
  (dec (:height window)))

(def scroll-keymaps
  {[:ctrl/y] (update-scroll - (constantly 1))
   [:ctrl/e] (update-scroll + (constantly 1))

   [:ctrl/b] (update-scroll - window-rows)
   [:ctrl/f] (update-scroll + window-rows)})

(def keymaps
  (merge
   mode-change-keymaps
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
