(ns saya.modules.input.insert-test
  (:require
   [cljs.test :refer-macros [deftest testing]]
   [saya.modules.input.insert :refer [backspace insert-at-cursor]]
   [saya.modules.input.test-helpers :refer [with-keymap-compare-buffer]]))

(deftest insert-key-at-buffer-test
  (testing "Insert key into empty buffer"
    (with-keymap-compare-buffer #(insert-at-cursor % "f")
      :empty
      "f|"))

  (testing "Insert key at end of first line"
    (with-keymap-compare-buffer #(insert-at-cursor % "o")
      "f|"
      "fo|")))

(deftest backspace-test
  (testing "Backspace no-op in empty buffer"
    (with-keymap-compare-buffer backspace
      :empty
      "|"))

  (testing "Backspace at end of line"
    (with-keymap-compare-buffer backspace
      "for the honor|"
      "for the hono|"))

  (testing "Backspace in the middle of line"
    (with-keymap-compare-buffer backspace
      "for the| honor"
      "for th| honor")))
