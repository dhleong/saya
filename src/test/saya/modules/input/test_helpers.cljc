(ns saya.modules.input.test-helpers)

(declare make-keymap-cofx)
(declare get-cofx-buffer)
(declare perform-cofx-key)
(declare ^:private do-feed-keys)
(declare ^:private do-find-error)

(defmacro has-no-error? [& where]
  (let [[opts & where] (if (map? (first where))
                         where
                         (cons {} where))]
    `(cljs.test/is (nil? (~'error ~opts))
                   (str "Expected no error" ~@where))))

(defmacro with-session [setup & body]
  `(let [cofx# (atom (make-keymap-cofx
                      ~(:buffer setup)))
         ~'state (fn
                   ([] @cofx#)
                   ([k# & ks#] (select-keys (:db @cofx#) (into [k#] ks#))))
         ~'buffer (comp (partial get-cofx-buffer)
                        ~'state)
         ~'mode (comp :mode ~'state)
         ~'error (partial do-find-error cofx#)
         ~'feed-keys (fn [the-keys# & {:keys [~'allow-error?]}]
                       (let [output# (do-feed-keys cofx# the-keys#)]
                         (has-no-error? {:types #{:exception}}
                                        " feeding keys: " the-keys#)
                         output#))
         ; Ignore unused keys:
         ~'_ [~'mode ~'feed-keys ~'buffer ~'error]]
     ~@body))

(defmacro has-error? [error-match]
  (if (string? error-match)
    `(cljs.test/is (= ~error-match (~'error)))
    `(let [~'error-message (~'error)]
       (cljs.test/is (string? ~'error-message))
       (cljs.test/is (re-seq ~error-match ~'error-message)
                     (str "Expected error `" ~'error-message "` to match " ~error-match)))))
