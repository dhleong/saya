(ns saya.util.coll-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [saya.util.coll :refer [insert-into-vec]]))

(deftest insert-into-vec-test
  (testing "Insert into empty vec"
    (is (= [1 2 3]
           (insert-into-vec
            []
            0
            [1 2 3]))))

  (testing "Insert at start of vec"
    (is (= [1 2 3 4 5 6]
           (insert-into-vec
            [4 5 6]
            0
            [1 2 3]))))

  (testing "Insert at middle of vec"
    (is (= [1 2 3 4 5 6]
           (insert-into-vec
            [1 2 5 6]
            2
            [3 4]))))

  (testing "Insert at end of vec"
    (is (= [1 2 3 4 5 6]
           (insert-into-vec
            [1 2 3]
            3
            [4 5 6])))))

