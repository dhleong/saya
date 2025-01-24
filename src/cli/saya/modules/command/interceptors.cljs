(ns saya.modules.command.interceptors
  (:require
   [re-frame.core :refer [->interceptor assoc-coeffect get-coeffect]]))

(defn aliases [& command-aliases]
  (->interceptor
   {:id :command/aliases
    :comment (vec command-aliases)}))

(def with-buffer-context
  ; Provides :bufnr, :winnr, :connr as cofx
  (->interceptor
   {:id :command/buffer-context
    :before (fn [context]
              (let [db (get-coeffect context :db {})
                    winnr (get db :current-winnr)
                    bufnr (get-in db [:windows winnr :bufnr])
                    connr (get-in db [:buffers bufnr :connection-id])]
                (cond-> context
                  (some? winnr)
                  (assoc-coeffect :winnr winnr)

                  (some? bufnr)
                  (assoc-coeffect :bufnr bufnr)

                  (some? connr)
                  (assoc-coeffect :connr connr))))}))
