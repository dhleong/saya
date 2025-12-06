(ns saya.modules.input.test-helpers
  (:require
   ["strip-ansi" :default strip-ansi]
   [clojure.string :as str]
   [clojure.test :refer [is]]
   [saya.cli.text-input.helpers :refer [split-text-by-state]]
   [saya.db :refer [default-db]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.buffers.line :refer [buffer-line]]
   [saya.modules.input.core :refer [handle-on-key]]
   [saya.modules.input.helpers :refer [*mode*]]
   [saya.modules.input.insert :refer [line->string]]
   [saya.modules.window.subs :refer [visible-lines]]))

(defn- extract-lines-and-cursor [s]
  (loop [raw-lines (str/split-lines s)
         indent nil
         lines []
         cursor {:row 0}]
    (if-let [line (first raw-lines)]
      (let [indent (or indent
                       (re-find #"^[ ]+" line))
            line (cond-> line
                   (some? indent) (subs (count indent)))
            cursor-col (str/index-of (strip-ansi line) "|")]
        (recur (next raw-lines)
               indent
               (conj lines (str/replace line #"\|" ""))
               (cond
                 cursor-col
                 (assoc cursor :col cursor-col)

                 (not (:col cursor))
                 (update cursor :row inc)

                 :else cursor)))
      (do
        (when-not (:col cursor)
          (throw (ex-info "No cursor provided in buffer: " {:lines lines})))
        [lines cursor]))))

(defn str->buffer [s]
  (let [[lines cursor] (extract-lines-and-cursor s)]
    {:lines (mapv buffer-line lines)
     :cursor cursor}))

(defn- insert-cursor [s cursor]
  (let [[before after] (split-text-by-state {:cursor cursor} s)]
    (str before "|" after)))

(defn buffer->vec [{:keys [lines cursor]}]
  (->> lines
       (keep-indexed (fn [i line]
                       (let [s (line->string line)]
                         (when (seq s)
                           (cond-> s
                             (= i (:row cursor))
                             (insert-cursor cursor))))))
       (into [])))

(defn make-context [& {:keys [buffer window]}]
  {:buffer (or (when (and buffer (not= :empty buffer))
                 (str->buffer buffer))
               (-> (buffer-events/create-blank default-db)
                   (second)
                   :buffer))
   :window (merge {:height 2 :width 20} window)
   :mode :normal})

(defn get-buffer [ctx]
  (-> (get-in ctx [:buffer])
      (select-keys [:lines :cursor])))

(defn with-keymap-compare-buffer
  [f buffer-before buffer-after & {:keys [window window-expect pending-operator
                                          mode-expect]}]
  (let [ctx (-> (make-context :buffer buffer-before
                              :window window)
                (assoc :pending-operator pending-operator))
        ctx' (try (binding [*mode* (:mode ctx)]
                    (f ctx))
                  (catch :default e
                    (println "ERROR performing " f ": " e)
                    (println (.-stack e))
                    (throw e)))

        expected (buffer->vec (get-buffer (make-context :buffer buffer-after)))
        actual (buffer->vec (get-buffer ctx'))]
    (is (= expected actual)
        (str "From " (buffer->vec (get-buffer ctx)) "\n"))

    (when window-expect
      (is (= window-expect (-> (get-in ctx' [:window])
                               (select-keys (keys window-expect))))
          (letfn [(vis' [ctx]
                    (->> (visible-lines
                          (:window ctx)
                          (:lines (:buffer ctx)))
                         (map (fn [{:keys [line]}]
                                (str/join line)))))]
            (str "Visible lines:\n"
                 (vis' ctx)
                 "\n -> \n"
                 (vis' ctx')
                 "\n Expected:\n"
                 (vis' (update ctx :window merge window-expect))))))

    (when mode-expect
      (is (= mode-expect
             (:mode ctx' (:mode ctx)))))))

(defn make-keymap-cofx [buffer]
  {:db {:buffers {0 (str->buffer buffer)}
        :windows {0 {:bufnr 0}}
        :mode :normal}
   :bufnr 0
   :winnr 0})

(defn perform-cofx-key [cofx key]
  (-> cofx
      (handle-on-key [key])
      (merge (select-keys cofx [:bufnr :winnr]))))

(defn get-cofx-buffer [cofx]
  (->> (get-in cofx [:db :buffers 0])
       (buffer->vec)
       (str/join "\n")))
