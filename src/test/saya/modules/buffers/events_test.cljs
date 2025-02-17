(ns saya.modules.buffers.events-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [saya.modules.buffers.events :refer [append-text clear-partial-line
                                        new-line]]))

(defn- empty-buffer []
  {:lines []})

(deftest append-text-test
  (testing "Append initial chunk of text"
    (let [initial-buffer (empty-buffer)]
      (is (= {:lines [[{:ansi "hello"}]]}

             (-> initial-buffer
                 (new-line)
                 (append-text {:ansi "hello"})))))))

(deftest clear-partial-line-test
  (testing "Replace a partial line"
    (let [inital-buffer (empty-buffer)]
      (is (= {:lines [[{:ansi "hello there"}]]}

             (-> inital-buffer
                 (new-line)
                 (append-text {:ansi "hello"})
                 (clear-partial-line)
                 (append-text {:ansi "hello there"})))))))
