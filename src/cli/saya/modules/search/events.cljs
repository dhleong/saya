(ns saya.modules.search.events
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx trim-v]]
   [saya.modules.buffers.line :refer [buffer-line]]
   [saya.modules.command.interceptors :refer [with-buffer-context]]
   [saya.modules.echo.core :refer [echo-fx]]
   [saya.modules.input.helpers :refer [adjust-scroll-to-cursor]]
   [saya.modules.search.core :as search]))

(reg-event-db
 ::prepare-buffer
 [trim-v]
 (fn [db [pending-input-line]]
   (assoc-in
    db
    [:buffers :search]
    {:lines (-> (get-in db [:histories :search])
                (->> (map buffer-line))
                (reverse)
                (vec)
                (conj (buffer-line pending-input-line)))})))

(reg-event-fx
 ::update-incremental
 [with-buffer-context trim-v]
 (fn [{:keys [db bufnr winnr]} [query]]
   (let [buffer (get-in db [:buffers bufnr])
         window (get-in db [:windows winnr])
         results (when (seq query)
                   (search/in-buffer buffer (get-in db [:search :direction]) query))
         next-result (first results)
         updated (when (seq results)
                   (adjust-scroll-to-cursor {:buffer (assoc buffer :cursor (:at next-result))
                                             :window window}))]
     {:db (cond-> db
            :always
            (update :search merge {:query query
                                   :results {bufnr results}})

            ; Capture original scroll position, if we don't already have it, so
            ; we can go back if cancel'd
            (not (get-in db [:search :original-window-state winnr] window))
            (update :search merge {:original-window {winnr window}
                                   :original-cursor {bufnr (:cursor buffer)}})

            (and (some? (:window updated))
                 (not= window (:window updated)))
            (assoc-in [:windows winnr] (:window updated)))})))

(reg-event-fx
 ::submit
 [with-buffer-context trim-v]
 (fn [{:keys [db bufnr]} [query]]
   ; TODO: apply any pending operator
   (let [results (get-in db [:search :results bufnr])
         result (first results)]
     {:db (-> db
              (assoc :mode :normal)
              (update :buffers dissoc :search)
              (cond->
               (some? result)
                (-> (assoc-in [:buffer bufnr :cursor] (:at result)))))
      :fx [(when-not (seq results)
             (echo-fx :error "Pattern not found:" query))]})))
