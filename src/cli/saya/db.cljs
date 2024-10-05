(ns saya.db)

(def default-db
  {:page [:home]
   :mode :normal
   :backstack []

   :buffers {}
   :windows {}

   :connection->bufnr {}

   :current-window nil
   :next-bufnr 0
   :next-winnr 0})

