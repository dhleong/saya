(ns saya.modules.echo.events
  (:require
   [applied-science.js-interop :as j]
   [clojure.string :as str]
   [re-frame.core :refer [inject-cofx reg-event-db reg-event-fx trim-v]]
   [saya.config :as config]))

(reg-event-fx
 ::ack-echo
 [(inject-cofx :now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (dissoc :echo-ack-pending-since)
            (assoc :echo-cleared-at now))}))

(reg-event-db
 ::reveal-latest
 (fn [db _]
   (assoc
    db
    :echo-ack-pending-since
    (->> (subvec
          (:echo-history db)
          (max 0
               (- (count (:echo-history db))
                  15)))
         (first)
         :timestamp
         dec))))

(defn- transform-message-part [part]
  (cond
    (ex-data part)
    [(ex-message part)
     "\nContext:\n"
     (ex-data part)
     (j/get part :stack)]

    (ex-message part)
    [(ex-message part)
     (j/get part :stack)]

    :else [part]))

(reg-event-fx
 :echo
 [trim-v (inject-cofx :now)]
 (fn [{:keys [db now]} message]
   (let [first-part (first message)
         message-config (cond
                          (map? first-part) first-part
                          (keyword? first-part) {:type first-part})
         message (if (some? message-config)
                   (next message)
                   message)

         message (mapcat transform-message-part message)

         last-echo (peek (:echo-history db))
         new-entries (->> message
                          (str/join " ")
                          (str/split-lines)
                          (map (fn [msg]
                                 (merge
                                  message-config
                                  {:timestamp now
                                   ; OOF This is not very pure, but...
                                   ; let's 80/20
                                   :key [now (js/Math.random)]
                                   :message msg}))))]
     {:db (-> db
              (update :echo-history (fnil into []) new-entries)

              (assoc :echo-ack-pending-since
                     (or (:echo-ack-pending-since db)
                         (when (or (< (- now
                                         (:timestamp last-echo))
                                      config/echo-prompt-window-ms)

                                   (> (count new-entries) 1))
                           now))))})))

(comment
  (do (re-frame.core/dispatch [:echo "For the honor"])
      (re-frame.core/dispatch [:echo "of grayskull!"]))

  (re-frame.core/dispatch [:echo :error "UH OH"])

  (re-frame.core/dispatch [:echo "hi\nthere"]))

