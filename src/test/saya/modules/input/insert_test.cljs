(ns saya.modules.input.insert-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [saya.db :refer [default-db]]
   [saya.modules.buffers.events :as buffer-events]
   [saya.modules.input.insert :refer [insert-at-buffer]]
   [saya.modules.input.test-helpers :refer [str->buffer]]))

(defn create-cofx
  ([] (create-cofx {}))
  ([& {:keys [buffer]}]
   (let [[db {created-buffer :buffer}] (-> default-db
                                           (buffer-events/create-blank))
         bufnr (:id created-buffer)]
     {:db (cond-> db
            buffer (update-in [:buffers bufnr] merge (str->buffer buffer)))
      :bufnr bufnr})))

(defn- get-buffer
  ([cofx] (get-buffer cofx 0))
  ([cofx id]
   (-> (get-in cofx [:db :buffers id])
       (dissoc :id))))

(deftest insert-key-at-buffer-test
  (testing "Insert key into empty buffer"
    (let [cofx (create-cofx)
          cofx' (insert-at-buffer cofx "f")]
      (is (= (str->buffer "f|")
             (get-buffer cofx')))))

  (testing "Insert key at end of first line"
    (let [cofx (create-cofx :buffer "f|")
          cofx' (insert-at-buffer cofx "o")]
      (is (= (str->buffer "fo|")
             (get-buffer cofx'))))))

