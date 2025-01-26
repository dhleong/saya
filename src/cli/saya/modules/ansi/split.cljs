(ns saya.modules.ansi.split
  (:require
   ["ansi-regex" :default ansi-regex]))

(defn chars-with-ansi [s]
  {:pre [(string? s)]}
  (let [regex (ansi-regex)]
    (loop [start 0
           pending-ansi nil
           result []]
      (let [m (.exec regex s)
            ansi (get m 0)
            end (if ansi
                  (.-lastIndex regex)
                  (count s))
            without-ansi (subs s start (- end
                                          (count ansi)))
            new-result (if (and (seq without-ansi) pending-ansi)
                         (-> result
                             (conj (str pending-ansi
                                        (first without-ansi)))
                             (into (next without-ansi)))

                         (into result without-ansi))]
        (cond
          ansi
          (recur end
                 ansi
                 new-result)

          ; If there's a pending ansi at the end, tack it onto
          ; the last visible character
          (and pending-ansi (seq new-result))
          (update new-result (dec (count new-result)) str pending-ansi)

          pending-ansi
          [pending-ansi]

          :else
          new-result)))))
