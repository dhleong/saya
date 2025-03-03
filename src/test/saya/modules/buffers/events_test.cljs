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
                 (append-text {:ansi "hello"}))))))

  (testing "Continue ansi on subsequent lines"
    (let [initial-buffer (empty-buffer)]
      (is (= ["\u001b[32mfor the"
              "\u001b[32mhonor of"]

             (-> initial-buffer
                 (new-line)
                 (append-text {:ansi "\u001b[32mfor the"})
                 (new-line)
                 (append-text {:ansi "honor of"})
                 (->> :lines (map str))))))))

(deftest clear-partial-line-test
  (testing "Replace a partial line"
    (let [inital-buffer (empty-buffer)]
      (is (= {:lines [[{:ansi "hello there"}]]}

             (-> inital-buffer
                 (new-line)
                 (append-text {:ansi "hello"})
                 (clear-partial-line)
                 (append-text {:ansi "hello there"})))))))
