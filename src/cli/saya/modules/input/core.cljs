(ns saya.modules.input.core
  (:require
   [clojure.core.match :refer [match]]
   [re-frame.core :refer [reg-event-fx trim-v]]
   [saya.modules.command.interceptors :refer [with-buffer-context]]
   [saya.modules.input.fx :as fx]
   [saya.modules.input.keymaps :as keymaps]
   [saya.modules.input.normal :as normal]))

(defn- get-current-cmdline [db bufnr]
  (let [{:keys [lines cursor]} (get-in db [:buffers bufnr])
        current-line (nth lines (:row cursor))]
    (apply str (map :ansi current-line))))

(reg-event-fx
 ::submit-cmdline
 [with-buffer-context]
 (fn [{:keys [db bufnr winnr]}]
   (let [to-submit (get-current-cmdline db bufnr)
         on-submit (get-in db [:windows winnr :on-submit])]
     {:dispatch [:command/quit]
      ::fx/perform-on-submit [on-submit to-submit]})))

(reg-event-fx
 ::cancel-cmdline
 [with-buffer-context]
 (fn [{:keys [db bufnr]}]
   ; TODO: Restore non-cmd input window?
   {:db (when (= :cmd bufnr)
          (assoc db :mode :command))
    :dispatch [:command/quit]}))

(reg-event-fx
 ::on-key
 [with-buffer-context trim-v]
 (fn [{{:keys [mode keymap-buffer] :as db} :db
       :keys [bufnr connr winnr]
       :as cofx}
      [key]]
   (match [mode key {:bufnr? (some? bufnr)
                     :submit? (some? (get-in db [:windows winnr :on-submit]))}]
     [:normal ":" _] {:db (assoc db :mode :command)}

     [:normal :return {:submit? true}] {:dispatch [::submit-cmdline]}
     [:insert :return {:submit? true}] {:dispatch [::submit-cmdline]}
     [:normal :ctrl/c {:submit? true}] {:dispatch [::cancel-cmdline]}
     [:insert :ctrl/c {:submit? true}] {:dispatch [::cancel-cmdline]}

     [:normal "i" {:bufnr? true}] {:db (assoc db :mode :insert)}

     [:normal key {:bufnr? true}]
     (let [new-buffer ((fnil conj []) keymap-buffer key)
           keymap (get normal/keymaps new-buffer)]
       (cond
         keymap
         (keymaps/perform cofx keymap)

         (keymaps/possible? db :normal new-buffer)
         {:db (assoc db :keymap-buffer new-buffer)}

         :else
         {:db (dissoc db :keymap-buffer)}))

     [:command :escape _] {:db (-> db
                                   ; Always clear:
                                   (assoc :mode :normal)
                                   (update :buffers dissoc :cmd))}
     [:command :ctrl/c _] {:db (-> db
                                   ; Always clear:
                                   (assoc :mode :normal)
                                   (update :buffers dissoc :cmd))}

; TODO: If we're in an input window, that should be handled
     ; special somehow (escaping to normal mode should not cause
     ; us to leave that input window!)
     [:insert :escape _] {:db (assoc db :mode :normal)}
     [:insert :ctrl/c _] {:db (-> db
                                  (assoc :mode :normal)
                                  (update :buffers dissoc [:conn/input connr]))}

     :else nil
     #_{:fx [[:log ["unhandled: " mode key]]]})))
