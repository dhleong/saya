(ns saya.cli.text-input.helpers)

(defn split-text-by-state [{:keys [cursor]} value]
  [(subs value 0 cursor)
   (subs value cursor)])

