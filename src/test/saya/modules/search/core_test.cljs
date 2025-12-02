(ns saya.modules.search.core-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [saya.modules.buffers.line :refer [buffer-line]]
   [saya.modules.search.core :refer [in-buffer in-string]]))

(deftest search-in-string-test
  (testing "Handle negative case"
    (is (nil?
         (in-string "bacon ipsum al pastor"
                    :newer
                    "birria"))))

  (testing "Simple search"
    (is (= [{:at {:col 0}
             :length 9}]
           (in-string "al pastor"
                      :newer
                      "al pastor")))
    (is (= [{:at {:col 12}
             :length 9}]
           (in-string "bacon ipsum al pastor"
                      :newer
                      "al pastor"))))

  (testing "Multiple Results in a line"
    (is (= [{:at {:col 0}
             :length 2}
            {:at {:col 2}
             :length 2}
            {:at {:col 4}
             :length 2}]
           (in-string "alalal"
                      :newer
                      "al")))
    (is (= [{:at {:col 4}
             :length 2}
            {:at {:col 2}
             :length 2}
            {:at {:col 0}
             :length 2}]
           (in-string "alalal"
                      :older
                      "al"))))

  (testing "Ansi search"
    (is (= [{:at {:col 0}
             :length 9}]
           (in-string "\u001b[32mal pastor"
                      :newer
                      "al pastor"))))

  (testing "Mixed-in Ansi search"
    (is (= [{:at {:col 0}
             :length 9}]
           (in-string "\u001b[32mal \u001b[33mpastor"
                      :newer
                      "al pastor")))))

(deftest search-in-buffer-test
  (testing "Order reverse search results correctly"
    (is (= [{:at {:col 5
                  :row 1}
             :length 7}
            {:at {:col 3
                  :row 0}
             :length 7}]
           (in-buffer
            {:lines [(buffer-line "my burrito")
                     (buffer-line "your burrito")]
             :cursor {:row 2}}
            :older
            "burrito"))))

  (testing "Start at the cursor, older"
    (is (= [{:at {:col 3
                  :row 0}
             :length 7}]
           (in-buffer
            {:lines [(buffer-line "my burrito")
                     (buffer-line "our burrito")
                     (buffer-line "your burrito")]
             :cursor {:row 1}}
            :older
            "burrito")))

    ; start on a match
    (is (= [{:at {:col 0
                  :row 1}
             :length 7}
            {:at {:col 0
                  :row 0}
             :length 7}]
           (in-buffer
            {:lines [(buffer-line "burrito")
                     (buffer-line "burrito")
                     (buffer-line "burrito")]
             :cursor {:row 2
                      :col 0}}
            :older
            "burrito"))))

  (testing "Start at the cursor, newer"
    (is (= [{:at {:col 4
                  :row 1}
             :length 7}
            {:at {:col 5
                  :row 2}
             :length 7}]
           (in-buffer
            {:lines [(buffer-line "my burrito")
                     (buffer-line "our burrito")
                     (buffer-line "your burrito")]
             :cursor {:row 1}}
            :newer
            "burrito")))

    (is (= [{:at {:col 0
                  :row 1}
             :length 7}
            {:at {:col 0
                  :row 2}
             :length 7}]
           (in-buffer
            {:lines [(buffer-line "burrito")
                     (buffer-line "burrito")
                     (buffer-line "burrito")]
             :cursor {:row 0
                      :col 0}}
            :newer
            "burrito"))
        "starting on a match")

    (is (= [{:at {:col 0
                  :row 1}
             :length 7}]
           (in-buffer
            {:lines [(buffer-line "burrito burrito")
                     (buffer-line "burrito")]
             :cursor {:row 0
                      :col 8}}
            :newer
            "burrito"))
        "starting on the last of multiple matches in a line")))
