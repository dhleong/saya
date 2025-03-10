(ns saya.cli.args
  (:require
   ["node:fs" :rename {existsSync exists-sync?}]
   [clojure.string :as str]
   [saya.util.paths :as paths]))

(def help
  (->> ["usage: saya [<uri>]"
        ""
        "options:"
        "   uri: Either a path to a script file or a"
        "        host:port to connect to."]
       (str/join "\n")))

(defn parse-cli-args [args]
  (when (some #{"--help"} args)
    (println help)
    (js/process.exit 0))

  (let [before-node (drop-while
                     (fn [p]
                       (not (str/ends-with? p "node")))
                     args)
        args (if (seq before-node)
               (drop 2 before-node)
               (drop 1 args))
        uri (first args)]
    (if (and (some? uri)
             (str/includes? uri ":"))
      [:connect uri]

      (if (exists-sync? (paths/resolve-user uri))
        [:load-script uri]

        (do
          (println "saya: No such script file: " uri)
          (js/process.exit 1))))))
