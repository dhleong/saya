(ns saya.db)

; TODO: Define structure of :buffer, :window, etc.

(def default-db
  {:page [:home]
   :mode :normal
   :backstack []

   :buffers {}
   :windows {}

   :connections {}

   :current-winnr nil
   :next-bufnr 0
   :next-winnr 0

   :echo-history []
   :echo-cleared-at nil
   :echo-ack-pending-since nil})

