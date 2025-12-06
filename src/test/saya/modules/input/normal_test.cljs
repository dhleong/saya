(ns saya.modules.input.normal-test
  (:require
   [cljs.test :refer-macros [deftest testing]]
   [saya.modules.input.helpers :refer [update-cursor]]
   [saya.modules.input.normal :as normal :refer [delete-operator update-scroll]]
   [saya.modules.input.test-helpers :refer [with-keymap-compare-buffer]]))

(defn with-keys-compare-buffer [keys before after & args]
  (let [f (get normal/keymaps keys)]
    (assert f (str "Unbound keymap keys: " keys))
    (apply with-keymap-compare-buffer f before after args)))

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
      "For the honor of Grayskul|l")))

(deftest scroll-test
  (testing "Scroll up single lines"
    (with-keymap-compare-buffer (update-scroll - (constantly 1))
      "For the
       honor of
       |Grayskull!"
      "For the
       |honor of
       Grayskull!"
      :window {:height 1 :width 10}
      :window-expect {:height 1 :anchor-row 1})

    (with-keymap-compare-buffer (update-scroll - (constantly 1))
      "For the
       |honor of
       Grayskull!"
      "|For the
       honor of
       Grayskull!"
      :window {:height 1 :width 10
               :anchor-row 1}
      :window-expect {:height 1 :anchor-row 0})

    ; no-op at the top
    (with-keymap-compare-buffer (update-scroll - (constantly 1))
      "|For the
       honor of
       Grayskull!"
      "|For the
       honor of
       Grayskull!"
      :window {:height 1 :width 10
               :anchor-row 0}
      :window-expect {:height 1 :anchor-row 0}))

  (testing "Scroll down single lines"
    (with-keymap-compare-buffer (update-scroll + (constantly 1))
      "|For the
       honor of
       Grayskull!"
      "For the
       |honor of
       Grayskull!"
      :window {:height 1 :width 10
               :anchor-row 0}
      :window-expect {:height 1 :anchor-row 1})

    (with-keymap-compare-buffer (update-scroll + (constantly 1))
      "For the
       |honor of
       Grayskull!"
      "For the
       honor of
       |Grayskull!"
      :window {:height 1 :width 10
               :anchor-row 1}
      :window-expect {:height 1})

    ; no-op at the bottom
    (with-keymap-compare-buffer (update-scroll + (constantly 1))
      "For the
       honor of
       |Grayskull!"
      "For the
       honor of
       |Grayskull!"
      :window {:height 1 :width 10}
      :window-expect {:height 1})))

(deftest wrapped-scroll-test
  (testing "Move cursor up with wrapped lines"
    (with-keymap-compare-buffer (update-scroll - (constantly 1))
      "For the honor of |Grayskull!"
      "For the |honor of Grayskull!"
      :window {:height 1 :width 10}
      :window-expect {:height 1 :width 10
                      :anchor-row 0
                      :anchor-offset 1})

    (with-keymap-compare-buffer (update-scroll - (constantly 1))
      "For the |honor of Grayskull!"
      "|For the honor of Grayskull!"
      :window {:height 1 :width 10
               :anchor-row 0
               :anchor-offset 1}
      :window-expect {:height 1 :width 10
                      :anchor-row 0
                      :anchor-offset 2})

    ; no-op at the top
    (with-keymap-compare-buffer (update-scroll - (constantly 1))
      "|For the honor of Grayskull!"
      "|For the honor of Grayskull!"
      :window {:height 1 :width 10
               :anchor-row 0 :anchor-offset 2}
      :window-expect {:height 1 :width 10
                      :anchor-row 0
                      :anchor-offset 2})))

(deftest vertical-cursor-movement-test
  (testing "Move cursor up with single lines"
    (with-keymap-compare-buffer (update-cursor :row dec)
      "For the
       honor of
       |Grayskull!"
      "For the
       |honor of
       Grayskull!"
      :window {:height 1 :width 10}
      :window-expect {:height 1 :anchor-row 1})

    (with-keymap-compare-buffer (update-cursor :row dec)
      "For the
       |honor of
       Grayskull!"
      "|For the
       honor of
       Grayskull!"
      :window {:height 1 :width 10
               :anchor-row 1}
      :window-expect {:height 1 :anchor-row 0})

    ; no-op at the top
    (with-keymap-compare-buffer (update-cursor :row dec)
      "|For the
       honor of
       Grayskull!"
      "|For the
       honor of
       Grayskull!"
      :window {:height 1 :width 10
               :anchor-row 0}
      :window-expect {:height 1 :width 10
                      :anchor-row 0}))

  (testing "Move cursor up with mixed lines"
    (with-keymap-compare-buffer (update-cursor :row dec)
      "Talkin
       away I
       |don't know
       what I'm to say
       I'll say it anyway"
      "Talkin
       |away I
       don't know
       what I'm to say
       I'll say it anyway"
      :window {:height 3 :width 10
               :anchor-row 3 :anchor-offset 1}
      :window-expect {:anchor-row 2 :anchor-offset 0})))

(deftest edit-keymaps-test
  (testing "Delete to eol"
    (with-keys-compare-buffer ["D"]
      "Talkin |away"
      "Talkin| "
      :mode-expect :normal))

  (testing "Change to eol"
    (with-keys-compare-buffer ["C"]
      "Talkin |away"
      "Talkin |"
      :mode-expect :insert))

  (testing "Delete char forward"
    (with-keys-compare-buffer ["x"]
      "Talkin awa|y"
      "Talkin aw|a"))
  (testing "Delete char backward"
    (with-keys-compare-buffer ["X"]
      "Talkin awa|y"
      "Talkin aw|y")))
