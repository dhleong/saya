(ns saya.util.hooks)

#? (:cljs
    (declare use-effect use-mount-effect))

(defmacro use-mount-effect! [& body]
  `(use-mount-effect (fn [] ~@body)))
