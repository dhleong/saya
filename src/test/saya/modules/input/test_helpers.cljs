(ns saya.modules.input.test-helpers
  (:require
   [clojure.string :as str]))

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
               (conj lines (str/replace line #"|" ""))
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
    {:lines (map (fn [line-str]
                   {:ansi line-str
                    :plain line-str})
                 lines)
     :cursor cursor}))

(defn make-context [& {:keys [buffer window]}]
  {:buffer (str->buffer buffer)
   :window (merge {:height 2} window)})
