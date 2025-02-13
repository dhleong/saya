(ns saya.modules.input.motions.word-test
  (:require
   [cljs.test :refer-macros [deftest testing]]
   [saya.modules.input.motions.word :refer [small-word-boundary? word-movement]]
   [saya.modules.input.test-helpers :refer [with-keymap-compare-buffer]]))

(deftest word-motion-test
  (testing "Small word movement forward"
    (with-keymap-compare-buffer (word-movement inc small-word-boundary?)
      "|For the honor of Grayskull!"
      "For |the honor of Grayskull!")

    (with-keymap-compare-buffer (word-movement inc small-word-boundary?)
      "For the honor of |Grayskull"
      "For the honor of Grayskul|l")

    (with-keymap-compare-buffer (word-movement inc small-word-boundary?)
      "For the ho|nor
       of Grayskull!"
      "For the honor
       |of Grayskull!"))

  (testing "Small word movement backward"
    (with-keymap-compare-buffer (word-movement dec small-word-boundary?)
      "For the honor of Grayskul|l"
      "For the honor of |Grayskull")

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
