(ns clj-kondo.gen-macros.saya.modules.input.test-helpers)

(defmacro with-session {:clj-kondo/macroexpand-hook true} [setup & body] `(let [cofx# (atom (make-keymap-cofx ~(:buffer setup))) ~'state (fn ([] @cofx#) ([k# & ks#] (select-keys (:db @cofx#) (into [k#] ks#)))) ~'buffer (comp (partial get-cofx-buffer) ~'state) ~'mode (comp :mode ~'state) ~'feed-keys (fn [& keys#] (-> cofx# (swap! (fn [cofx'#] (reduce (fn [co'# k#] (let [n# (perform-cofx-key co'# k#)] (println n#) n#)) cofx'# keys#))) (get-cofx-buffer))) ~'_ [~'mode ~'feed-keys ~'buffer]] ~@body))
