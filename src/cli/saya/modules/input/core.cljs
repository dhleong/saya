(ns saya.modules.input.core
  (:require
   [clojure.core.match :refer [match]]
   [re-frame.core :refer [reg-event-fx trim-v]]
   [saya.modules.command.interceptors :refer [with-buffer-context]]
   [saya.modules.input.keymaps :as keymaps]
   [saya.modules.input.normal :as normal]))

(reg-event-fx
 ::on-key
 [with-buffer-context trim-v]
 (fn [{{:keys [mode keymap-buffer] :as db} :db
       :keys [bufnr]
       :as cofx}
      [key]]
   (match [mode key (some? bufnr)]
     [:normal ":" _] {:db (assoc db :mode :command)}

     ; TODO: In a connection buffer, this should open into an
     ; input window
     [:normal "i" true] {:db (assoc db :mode :insert)}
     [:normal "l" true] {:db (update-in db [:buffers bufnr :cursor :col] inc)}

     ; TODO: Probably reorganize, definitely clamp
     [:normal key true]
     (let [new-buffer ((fnil conj []) keymap-buffer key)
           keymap (get normal/keymaps new-buffer)]
       (cond
         keymap
         (keymaps/perform cofx keymap)

         (keymaps/possible? db :normal new-buffer)
         {:db (assoc db :keymap-buffer new-buffer)}

         :else
         {:db (dissoc db :keymap-buffer)}))

     [:command :escape _] {:db (assoc db :mode :normal)}

     ; TODO: If we're in an input window, that should be handled
     ; special somehow (escaping to normal mode should not cause
     ; us to leave that input window!)
     [:insert :escape _] {:db (assoc db :mode :normal)}

     :else
     {:fx [[:log ["unhandled: " mode key]]]})))
