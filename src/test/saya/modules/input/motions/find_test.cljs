(ns saya.modules.input.motions.find-test
  (:require
   [cljs.test :refer-macros [deftest testing]]
   [saya.modules.input.motions.find :refer [perform-find-ch perform-until-ch]]
   [saya.modules.input.test-helpers :refer [with-keymap-compare-buffer]]))

(deftest perform-find-ch-test
  (testing "Find character"
    (with-keymap-compare-buffer #(perform-find-ch % inc (partial = "t"))
      "|For the honor of Grayskull!"
      "For |the honor of Grayskull!"))

  (testing "Find character backward"
    (with-keymap-compare-buffer #(perform-find-ch % dec (partial = "o"))
      "For the honor |of Grayskull!"
      "For the hon|or of Grayskull!")))

(deftest perform-until-ch-test
  (testing "Until character"
    (with-keymap-compare-buffer #(perform-until-ch % inc (partial = "t"))
      "|For the honor of Grayskull!"
      "For| the honor of Grayskull!")

    (with-keymap-compare-buffer #(perform-until-ch % inc (partial = " "))
      "For |the honor of Grayskull!"
      "For th|e honor of Grayskull!"))

  (testing "Backwards until character"
    (with-keymap-compare-buffer #(perform-until-ch % dec (partial = " "))
      "For the| honor of Grayskull!"
      "For |the honor of Grayskull!")

    (with-keymap-compare-buffer #(perform-until-ch % dec (partial = " "))
      "For| the honor of Grayskull!"
      "|For the honor of Grayskull!")))
