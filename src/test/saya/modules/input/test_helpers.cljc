(ns saya.modules.input.test-helpers)

(declare make-keymap-cofx)
(declare get-cofx-buffer)
(declare perform-cofx-key)
(declare ^:private do-feed-keys)
(declare ^:private do-find-error)

(defmacro with-session [setup & body]
  `(let [cofx# (atom (make-keymap-cofx
                      ~(:buffer setup)))
         ~'state (fn
                   ([] @cofx#)
                   ([k# & ks#] (select-keys (:db @cofx#) (into [k#] ks#))))
         ~'buffer (comp (partial get-cofx-buffer)
                        ~'state)
         ~'mode (comp :mode ~'state)
         ~'feed-keys (partial do-feed-keys cofx#)
         ~'error (partial do-find-error cofx#)
         ; Ignore unused keys:
         ~'_ [~'mode ~'feed-keys ~'buffer ~'error]]
     ~@body))

(defmacro has-no-error? []
  `(cljs.test/is (nil? (~'error))
                 "Expected no error"))

(defmacro has-error? [error-match]
  (if (string? error-match)
    `(cljs.test/is (= ~error-match (~'error)))
    `(let [~'error-message (~'error)]
       (cljs.test/is (string? ~'error-message))
       (cljs.test/is (re-seq ~error-match ~'error-message)
                     (str "Expected error `" ~'error-message "` to match " ~error-match)))))
