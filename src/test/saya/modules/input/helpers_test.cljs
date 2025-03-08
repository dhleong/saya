(ns saya.modules.input.helpers-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [saya.modules.input.helpers :refer [adjust-scroll-to-cursor
                                       derive-anchor-from-top-cursor]]
   [saya.modules.input.normal :refer [scroll-to-top update-scroll]]
   [saya.modules.input.test-helpers :refer [make-context
                                            with-keymap-compare-buffer]]))

(defn derive-anchor-from-top-cursor' [{:keys [buffer window]}]
  (derive-anchor-from-top-cursor
   (:lines buffer)
   (:width window)
   (:cursor buffer)
   (:height window)))

(deftest derive-anchor-from-top-cursor-test
  (testing "Handle non-wrapped lines"
    (is (= {:anchor-row 1 :anchor-offset 0}
           (derive-anchor-from-top-cursor'
            (make-context
             :buffer "For the
                      |honor of
                      Greyskull"
             :window {:height 1 :width 10
                      :anchor-row 2})))))

  (testing "Handle wrapped lines"
    (is (= {:anchor-row 0 :anchor-offset 3}
           (derive-anchor-from-top-cursor'
            (make-context
             :buffer "|Talkin away I don't know
                      what I'm to say
                      I'll say it anyway"
             ; NOTE: window/height is the key difference between
             ; this test and the next
             :window {:height 1 :width 10}))))

    (is (= {:anchor-row 0 :anchor-offset 2}
           (derive-anchor-from-top-cursor'
            (make-context
             :buffer "|Talkin away I don't know
                      what I'm to say
                      I'll say it anyway"
             :window {:height 2 :width 10}))))))

(deftest adjust-scroll-to-cursor-test
  (testing "Scroll to bottom"
    (let [ctx (make-context
               :buffer "For the
                        honor of
                        |Greyskull"
               :window {:height 2 :anchor-row 1})
          ctx' (adjust-scroll-to-cursor ctx)]
      (is (nil? (:anchor-row (:window ctx'))))
      (is (= {:row 2 :col 0}
             (:cursor (:buffer ctx'))))))

  (testing "Scroll to top, wrapped"
    (let [ctx (make-context
               :buffer "|Talkin away I don't know
                        what I'm to say
                        I'll say it anyway"
               :window {:height 2 :width 10})
          ctx' (adjust-scroll-to-cursor ctx)]
      (is (= 0 (:anchor-row (:window ctx'))))
      (is (= 2 (:anchor-offset (:window ctx'))))))

  (testing "Scroll to top, mixed-wrapped"
    (let [ctx (make-context
               :buffer "|Talkin
                        away I
                        don't know
                        what I'm to say I'll
                        say it anyway"
               :window {:height 2 :width 10})
          ctx' (adjust-scroll-to-cursor ctx)]
      (is (= 1 (:anchor-row (:window ctx'))))
      (is (= 0 (:anchor-offset (:window ctx'))))))

  (testing "Scroll to top, non wrapped"
    (let [ctx (make-context
               :buffer "|Talkin away I don't know
                        what I'm to say
                        I'll say it anyway"
               :window {:height 2 :width 30})
          ctx' (adjust-scroll-to-cursor ctx)]
      (is (= 1 (:anchor-row (:window ctx'))))
      (is (= 0 (:anchor-offset (:window ctx'))))))

  (testing "Scroll to top, tall window"
    (with-keymap-compare-buffer scroll-to-top
      "Talkin
       away I
       don't know what I'm to say I'll
       say it |anyway"
      "|Talkin
       away I
       don't know what I'm to say I'll
       say it anyway"
      :window {:height 7 :width 10}
      :window-expect {:anchor-row 3 :anchor-offset 1})))

(deftest adjust-cursor-to-scroll-test
  (testing "Handle wrapped lines"
    (with-keymap-compare-buffer (update-scroll - (constantly 3))
      "Talkin
       away I
       don't know what I'm to say I'll
       say it |anyway"
      "Talkin
       away I
       don't know what |I'm to say I'll
       say it anyway"
      :window {:height 4 :width 10}
      :window-expect {:anchor-row 2 :anchor-offset 1})))
