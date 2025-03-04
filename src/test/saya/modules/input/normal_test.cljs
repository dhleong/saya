(ns saya.modules.input.normal-test
  (:require
   [cljs.test :refer-macros [deftest testing]]
   [saya.modules.input.normal :refer [delete-operator update-scroll]]
   [saya.modules.input.test-helpers :refer [with-keymap-compare-buffer]]))

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
      "|the honor of Grayskull!"))

  (testing "Inclusive delete"
    (with-keymap-compare-buffer #(delete-operator % {:start {:row 0 :col 0}
                                                     :end {:row 0 :col 2}
                                                     :inclusive? true})
      "For |the honor of Grayskull!"
      "| the honor of Grayskull!")

    (with-keymap-compare-buffer #(delete-operator % {:start (:cursor (:buffer %))
                                                     :end {:row 0 :col 27}
                                                     :inclusive? true})
      "For the honor of Grayskull|!"
      "For the honor of Grayskull|")))

(deftest scroll-test
  (testing "Scroll up single lines"
    (with-keymap-compare-buffer (update-scroll - (constantly 1))
      "For the
       honor of
       |Grayskull!"
      "For the
       |honor of
       Grayskull!"
      :window {:height 1}
      :window-expect {:height 1 :anchor-row 1})

    (with-keymap-compare-buffer (update-scroll - (constantly 1))
      "For the
       |honor of
       Grayskull!"
      "|For the
       honor of
       Grayskull!"
      :window {:height 1 :anchor-row 1}
      :window-expect {:height 1 :anchor-row 0})

    ; no-op at the top
    (with-keymap-compare-buffer (update-scroll - (constantly 1))
      "|For the
       honor of
       Grayskull!"
      "|For the
       honor of
       Grayskull!"
      :window {:height 1 :anchor-row 0}
      :window-expect {:height 1 :anchor-row 0}))

  (testing "Scroll down single lines"
    (with-keymap-compare-buffer (update-scroll + (constantly 1))
      "|For the
       honor of
       Grayskull!"
      "For the
       |honor of
       Grayskull!"
      :window {:height 1 :anchor-row 0}
      :window-expect {:height 1 :anchor-row 1})

    (with-keymap-compare-buffer (update-scroll + (constantly 1))
      "For the
       |honor of
       Grayskull!"
      "For the
       honor of
       |Grayskull!"
      :window {:height 1 :anchor-row 1}
      :window-expect {:height 1})

    ; no-op at the bottom
    (with-keymap-compare-buffer (update-scroll + (constantly 1))
      "For the
       honor of
       |Grayskull!"
      "For the
       honor of
       |Grayskull!"
      :window {:height 1}
      :window-expect {:height 1})))
