(ns saya.modules.input.keymaps-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [saya.modules.input.keymaps :refer [possible?]]))

(deftest possible?-test
  (testing "Multi-key possibilities"
    (is (possible?
         {["g" "a"] "go"}
         []))

    (is (possible?
         {["g" "a"] "go"}
         ["g"]))
    (is (possible?
         {["g" "a" "b"] "go"}
         ["g"]))
    (is (possible?
         {["g" "a" "b"] "go"}
         ["g" "a"])))

  (testing "Absurdity can't be possible (sorry)"
    (is (not (possible?
              {["g"] "go"}
              ["g" "a" "b"])))))

