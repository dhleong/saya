(ns clj-kondo.gen-macros.saya.modules.input.test-helpers)

(alias 'saya.modules.input.test-helpers 'clj-kondo.gen-macros.saya.modules.input.test-helpers)

(defmacro with-session {:clj-kondo/macroexpand-hook true} [setup & body] `(let [cofx# (atom (make-keymap-cofx ~(:buffer setup))) ~'state (fn ([] @cofx#) ([k# & ks#] (select-keys (:db @cofx#) (into [k#] ks#)))) ~'buffer (comp (partial get-cofx-buffer) ~'state) ~'mode (comp :mode ~'state) ~'feed-keys (partial do-feed-keys cofx#) ~'_ [~'mode ~'feed-keys ~'buffer]] ~@body))
