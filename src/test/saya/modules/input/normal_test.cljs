(ns saya.modules.input.normal-test
  (:require
   [cljs.test :refer-macros [deftest testing]]
   [saya.modules.input.normal :refer [delete-operator small-word-boundary?
                                      word-movement]]
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
      "For the
       |of Grayskull!")))

(deftest delete-operator-test
  (testing "Linewise delete"
    (with-keymap-compare-buffer #(delete-operator % {:start {:row 1 :col 2}
                                                     :end {:row 2 :col 2}
                                                     :linewise? true})
      "For the
       ho|nor
       of
       Grayskull!"
      "For the
       Gr|ayskull!")

    (with-keymap-compare-buffer #(delete-operator % {:start {:row 0 :col 0}
                                                     :end {:row 3 :col 0}
                                                     :linewise? true})
      "|For the
       honor
       of
       Grayskull!"
      :empty)

    (with-keymap-compare-buffer #(delete-operator % {:start {:row 3 :col 0}
                                                     :end {:row 0 :col 0}
                                                     :linewise? true})
      "For the
       honor
       of
       |Grayskull!"
      :empty))

  (testing "Charwise delete"
    (with-keymap-compare-buffer #(delete-operator % {:start {:row 0 :col 4}
                                                     :end {:row 0 :col 8}})
      "For |the honor of Grayskull!"
      "For |honor of Grayskull!")

    (with-keymap-compare-buffer #(delete-operator % {:start {:row 0 :col 4}
                                                     :end {:row 0 :col 0}})
      "For |the honor of Grayskull!"
      "|the honor of Grayskull!")))

