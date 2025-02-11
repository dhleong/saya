(ns saya.modules.input.test-helpers
  (:require
   [clojure.string :as str]
   [clojure.test :refer [is]]
   [saya.db :refer [default-db]]
   [saya.modules.buffers.events :as buffer-events]))

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
            cursor-col (str/index-of line "|")]
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
    {:lines (mapv (fn [line-str]
                    [{:ansi line-str
                      :plain line-str}])
                  lines)
     :cursor cursor}))

(defn make-context [& {:keys [buffer window]}]
  {:buffer (or (when (and buffer (not= :empty buffer))
                 (str->buffer buffer))
               (-> (buffer-events/create-blank default-db)
                   (second)
                   :buffer))
   :window (merge {:height 2} window)})

(defn get-buffer [ctx]
  (-> (get-in ctx [:buffer])
      (dissoc :id)))

(defn with-keymap-compare-buffer [f buffer-before buffer-after]
  (let [ctx (make-context :buffer buffer-before)
        ctx' (f ctx)]
    (is (= (str->buffer buffer-after)
           (get-buffer ctx')))))
