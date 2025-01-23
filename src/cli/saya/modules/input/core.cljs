(ns saya.modules.input.core
  (:require
   [clojure.core.match :refer [match]]
   [re-frame.core :refer [reg-event-fx trim-v]]
   [saya.modules.command.interceptors :refer [with-buffer-context]]))

(reg-event-fx
 ::on-key
 [with-buffer-context trim-v]
 (fn [{{:keys [mode] :as db} :db :keys [bufnr]} [key]]
   (match [mode key (some? bufnr)]
     [:normal ":" _] {:db (assoc db :mode :command)}

     ; TODO: Probably reorganize, definitely clamp
     [:normal "0" true] {:db (assoc-in db [:buffers bufnr :cursor :col] 0)}
     [:normal "l" true] {:db (update-in db [:buffers bufnr :cursor :col] inc)}
     [:normal "h" true] {:db (update-in db [:buffers bufnr :cursor :col] dec)}

     ; TODO: In a connection buffer, this should open into an
     ; input window
     [:normal "i" true] {:db (assoc db :mode :insert)}

     [:command :escape _] {:db (assoc db :mode :normal)}

     ; TODO: If we're in an input window, that should be handled
     ; special somehow (escaping to normal mode should not cause
     ; us to leave that input window!)
     [:insert :escape _] {:db (assoc db :mode :normal)}

     :else
     {:fx [[:log ["unhandled: " mode key]]]})))
