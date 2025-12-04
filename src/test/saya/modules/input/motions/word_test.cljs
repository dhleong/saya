(ns saya.modules.input.motions.word-test
  (:require
   [cljs.test :refer-macros [deftest testing]]
   [saya.modules.input.motions.word :refer [end-of-word-movement
                                            small-word-boundary? word-movement]]
   [saya.modules.input.test-helpers :refer [with-keymap-compare-buffer]]))

(deftest word-motion-test
  (testing "Small word movement forward"
    (with-keymap-compare-buffer (word-movement inc small-word-boundary?)
      "|For the honor of Grayskull!"
      "For |the honor of Grayskull!")

    (with-keymap-compare-buffer (word-movement inc small-word-boundary?)
      "|For-the honor of Grayskull!"
      "For|-the honor of Grayskull!")

    (with-keymap-compare-buffer (word-movement inc small-word-boundary?)
      "For|-the honor of Grayskull!"
      "For-|the honor of Grayskull!")

    (with-keymap-compare-buffer (word-movement inc small-word-boundary?)
      "For the honor of |Grayskull"
      "For the honor of Grayskul|l")

    (with-keymap-compare-buffer (word-movement inc small-word-boundary?)
      "For the ho|nor
       of Grayskull!"
      "For the honor
       |of Grayskull!"))

  (testing "Small word movement with ansi"
    (with-keymap-compare-buffer (word-movement inc small-word-boundary?)
      "|\u001b[32mFor the honor of Grayskull!"
      "\u001b[32mFor |the honor of Grayskull!"))

  (testing "Small word movement backward"
    (with-keymap-compare-buffer (word-movement dec small-word-boundary?)
      "For the honor of Grayskul|l"
      "For the honor of |Grayskull")

    (with-keymap-compare-buffer (word-movement dec small-word-boundary?)
      "For|-the honor of Grayskull!"
      "|For-the honor of Grayskull!")

    (with-keymap-compare-buffer (word-movement dec small-word-boundary?)
      "For-|the honor of Grayskull!"
      "For|-the honor of Grayskull!")

    (with-keymap-compare-buffer (word-movement dec small-word-boundary?)
      "For the honor of |Grayskull"
      "For the honor |of Grayskull")

    (with-keymap-compare-buffer (word-movement dec small-word-boundary?)
      "For the honor of| Grayskull"
      "For the honor |of Grayskull")

    (with-keymap-compare-buffer (word-movement dec small-word-boundary?)
      "For |the honor of Grayskull!"
      "|For the honor of Grayskull!")

    (with-keymap-compare-buffer (word-movement dec small-word-boundary?)
      "For the honor
       |of Grayskull!"
      "For the |honor
       of Grayskull!")))

(deftest end-of-word-test
  (testing "Small end-of-word movement forward"
    (with-keymap-compare-buffer (end-of-word-movement inc small-word-boundary?)
      "|For the honor of Grayskull!"
      "Fo|r the honor of Grayskull!")

    (with-keymap-compare-buffer (end-of-word-movement inc small-word-boundary?)
      "Fo|r the honor of Grayskull!"
      "For th|e honor of Grayskull!")

    (with-keymap-compare-buffer (end-of-word-movement inc small-word-boundary?)
      "|For-the honor of Grayskull!"
      "Fo|r-the honor of Grayskull!")

    ; TODO:
    #_(with-keymap-compare-buffer (end-of-word-movement inc small-word-boundary?)
        "Fo|r-the honor of Grayskull!"
        "For|-the honor of Grayskull!")

    (with-keymap-compare-buffer (end-of-word-movement inc small-word-boundary?)
      "For the honor of |Grayskull"
      "For the honor of Grayskul|l")

    (with-keymap-compare-buffer (end-of-word-movement inc small-word-boundary?)
      "For the hono|r
       of Grayskull!"
      "For the honor
       o|f Grayskull!"))

  (testing "Small end-of-word movement backward"
    (with-keymap-compare-buffer (end-of-word-movement dec small-word-boundary?)
      "For the honor of Grayskul|l"
      "For the honor o|f Grayskull")

    ; TODO:
    #_(with-keymap-compare-buffer (end-of-word-movement dec small-word-boundary?)
        "For|-the honor of Grayskull!"
        "Fo|r-the honor of Grayskull!")

    ; TODO:
    #_(with-keymap-compare-buffer (end-of-word-movement dec small-word-boundary?)
        "For-|the honor of Grayskull!"
        "For|-the honor of Grayskull!")

    (with-keymap-compare-buffer (end-of-word-movement dec small-word-boundary?)
      "For the honor of |Grayskull"
      "For the honor o|f Grayskull")

    (with-keymap-compare-buffer (end-of-word-movement dec small-word-boundary?)
      "For the honor o|f Grayskull"
      "For the hono|r of Grayskull")

    (with-keymap-compare-buffer (end-of-word-movement dec small-word-boundary?)
      "Fo|r the honor of Grayskull!"
      "|For the honor of Grayskull!")

    ; TODO:
    #_(with-keymap-compare-buffer (end-of-word-movement dec small-word-boundary?)
        "For the honor
       o|f Grayskull!"
        "For the hono|r
       of Grayskull!")))
