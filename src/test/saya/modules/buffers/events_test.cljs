(ns saya.modules.buffers.events-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [saya.modules.buffers.events :refer [append-text new-line]]))

(deftest append-text-test
  (testing "Append initial chunk of text"
    (let [initial-buffer {:lines []}]
      (is (= {:lines [[{:ansi "hello"
                        :parsed :parsed}]]}

             (-> initial-buffer
                 (new-line)
                 (append-text {:ansi "hello"
                               :parsed :parsed})))))))
