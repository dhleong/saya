(ns hooks.test-helpers)

(defmacro with-session [setup & body]
  `(let [cofx# (atom ~setup)
         ~'state (fn
                   ([] {})
                   ([k# & ks#] (select-keys
                                (:db @cofx#)
                                (into [k#] ks#))))
         ~'buffer (fn [] "")
         ~'mode (comp :mode ~'state)
         ~'feed-keys (fn [& ~'_] "")
         ~'error (fn [] "")
         ~'_ [~'mode ~'feed-keys ~'buffer ~'error]]
     ~@body))

