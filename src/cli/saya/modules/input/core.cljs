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
   [saya.modules.input.modes :refer [bufnr->mode]]
   [saya.modules.input.normal :as normal]
   [saya.modules.input.op :as op]
   [saya.modules.input.prompt :as prompt]))

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
   {:db (if-some [mode (bufnr->mode bufnr)]
          (assoc db :mode mode)
          db)
    :dispatch [:command/quit]}))

(defn- exit-insert-mode [{:keys [db] :as cofx}]
  (-> (keymaps/perform cofx (normal/with-editable
                              (update-cursor :col dec)))
      :db
      (or db)
      (assoc :mode (if (:connr cofx)
                     :prompt
                     :normal))))

(defn- perform-operator-pending [& {:keys [cofx db key return-mode]}]
  (-> (if (and
           (empty? (:keymap-buffer db)) ; Sanity check, really
           (= key (:char (meta (:pending-operator db)))))
        (keymaps/maybe-perform-with-keymap-buffer
         :mode :operator-pending
         :keymaps op/full-line-keymap
         :keymap-buffer (:keymap-buffer db)
         :key :full-line
         :cofx cofx)

        (keymaps/maybe-perform-with-keymap-buffer
         :mode :operator-pending
         :keymaps op/keymaps
         :keymap-buffer (:keymap-buffer db)
         :key key
         :cofx cofx))

      ; If we haven't explicitly set left :pending-operator mode,
      ; and there's no pending keymap-buffer, do so:
      (update :db
              (fn [{:keys [mode keymap-buffer] :as db'}]
                (cond-> (or db' db)
                  (and (= :operator-pending mode)
                       (nil? keymap-buffer))
                  (assoc :mode return-mode))))))

(defn handle-on-key [{{:keys [mode keymap-buffer] :as db} :db
                      :keys [bufnr connr winnr]
                      :as cofx}
                     [key]]
  (match [mode key {:bufnr? (some? bufnr)
                    :readonly? (or (some? connr)
                                   (buffers/readonly?
                                    (get-in db [:buffers bufnr])))
                    :connr? (some? connr)
                    :submit? (some? (get-in db [:windows winnr :on-submit]))}]
    [(:or :normal :prompt) ":" _] {:db (assoc db :mode :command)
                                   :fx [[:dispatch [::echo-events/ack-echo]]]}

    [(:or :normal :prompt) "/" _] {:db (-> db
                                           (assoc :mode :search)
                                           (assoc :search {:direction (if connr :older :newer)}))
                                   :fx [[:dispatch [::echo-events/ack-echo]]]}

    [(:or :normal :prompt) "?" _] {:db (-> db
                                           (assoc :mode :search)
                                           (assoc :search {:direction (if connr :newer :older)}))
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

    [:search :escape _] {:db (cond-> db
                               :always (assoc :mode :normal)
                               ; Only clear if we're not in the cmdline window
                               (not= :search bufnr) (update :buffers dissoc :search))}
    [:search :ctrl/c _] {:db (-> db
                                 ; Always clear:
                                 (assoc :mode :normal)
                                 (update :buffers dissoc :search))}

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
    (perform-operator-pending
     :cofx cofx
     :db db
     :key key
     :return-mode :normal)

    [:operator-pending key {:connr? true}]
    (perform-operator-pending
     :cofx (assoc cofx :bufnr [:conn/input connr]
                  :normal-bufnr bufnr)
     :db db
     :key key
     :return-mode :prompt)

    [:prompt :escape _] {:db (assoc db :mode :normal)}
    [:prompt :ctrl/c _] {:db (assoc db :mode :normal)}
    ; TODO: Support submitting from :prompt mode

    ; TODO: We'll need to ensure that :prompt mode doesn't persist
    ; when leaving a connr buffer...
    [:prompt key {:connr? true}]
    (keymaps/maybe-perform-with-keymap-buffer
     :mode :prompt
     :keymaps prompt/keymaps
     :keymap-buffer keymap-buffer
     :cofx (assoc cofx :bufnr [:conn/input connr]
                  :normal-bufnr bufnr)
     :key key)

    :else nil
    #_{:fx [[:log ["unhandled: " mode key]]]}))

(reg-event-fx
 ::on-key
 [with-buffer-context trim-v]
 handle-on-key)
