(ns saya.modules.input.core
  (:require
   [clojure.core.match :refer [match]]
   [re-frame.core :refer [reg-event-fx trim-v]]
   [saya.modules.buffers.util :as buffers]
   [saya.modules.command.interceptors :refer [with-buffer-context]]
   [saya.modules.echo.events :as echo-events]
   [saya.modules.input.fx :as fx]
   [saya.modules.input.helpers :refer [update-cursor]]
   [saya.modules.input.insert :as insert]
   [saya.modules.input.keymaps :as keymaps]
   [saya.modules.input.normal :as normal]
   [saya.modules.input.op :as op]))

(defn- get-current-cmdline [db bufnr]
  (let [{:keys [lines cursor]} (get-in db [:buffers bufnr])
        current-line (when (seq lines)
                       (nth lines (:row cursor)))]
    (str current-line)))

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

(defn- exit-insert-mode [{:keys [db] :as cofx}]
  (-> (keymaps/perform cofx (update-cursor :col dec))
      :db
      (or db)
      (assoc :mode :normal)))

(reg-event-fx
 ::on-key
 [with-buffer-context trim-v]
 (fn [{{:keys [mode keymap-buffer] :as db} :db
       :keys [bufnr connr winnr]
       :as cofx}
      [key]]
   (match [mode key {:bufnr? (some? bufnr)
                     :readonly? (or (some? connr)
                                    (buffers/readonly?
                                     (get-in db [:buffers bufnr])))
                     :submit? (some? (get-in db [:windows winnr :on-submit]))}]
     [:normal ":" _] {:db (assoc db :mode :command)
                      :fx [[:dispatch [::echo-events/ack-echo]]]}

     [:normal :return {:submit? true}] {:dispatch [::submit-cmdline]}
     [:insert :return {:submit? true}] {:dispatch [::submit-cmdline]}
     [:normal :ctrl/c {:submit? true}] {:dispatch [::cancel-cmdline]}
     [:insert :ctrl/c {:submit? true}] {:dispatch [::cancel-cmdline]}

     [:normal key {:bufnr? true}]
     (keymaps/maybe-perform-with-keymap-buffer
      :mode :normal
      :keymaps normal/keymaps
      :keymap-buffer keymap-buffer
      :cofx cofx
      :key key)

     [:command :escape _] {:db (cond-> db
                                 :always (assoc :mode :normal)
                                 ; Only clear if we're not in the cmdline window
                                 (not= :cmd bufnr) (update :buffers dissoc :cmd))}
     [:command :ctrl/c _] {:db (-> db
                                   ; Always clear:
                                   (assoc :mode :normal)
                                   (update :buffers dissoc :cmd))}

     [:insert :escape _] {:db (exit-insert-mode cofx)}
     [:insert :ctrl/c _] {:db (-> cofx
                                  (exit-insert-mode)
                                  (update :buffers dissoc [:conn/input connr]))}
     [:insert key {:bufnr? true
                   :readonly? false}]
     (keymaps/maybe-perform-with-keymap-buffer
      :mode :insert
      :keymaps insert/keymaps
      :keymap-buffer keymap-buffer
      :key key
      :cofx cofx
      :with-unhandled (fn [cofx]
                        (if (string? key)
                          (try
                            (keymaps/perform
                             cofx
                             #(insert/insert-at-cursor % key))
                            (catch :default e
                              (assoc cofx :fx [[:log ["error: " e]]])))

                          cofx)))

     [:operator-pending key {:bufnr? true
                             :readonly? false}]
     (->
      (if (= key (:char (meta (:pending-operator db))))
        (keymaps/maybe-perform-with-keymap-buffer
         :mode :operator-pending
         :keymaps op/full-line-keymap
         :key :full-line
         :cofx cofx)

        (keymaps/maybe-perform-with-keymap-buffer
         :mode :operator-pending
         :keymaps op/keymaps
         :key key
         :cofx cofx))
      (update :db (fnil assoc db) :mode :normal))

     :else nil
     #_{:fx [[:log ["unhandled: " mode key]]]})))
