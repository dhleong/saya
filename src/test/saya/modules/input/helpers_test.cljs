(ns saya.modules.input.helpers-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [saya.modules.input.helpers :refer [adjust-scroll-to-cursor]]
   [saya.modules.input.test-helpers :refer [make-context]]))

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
      (is (= 1 (:anchor-offset (:window ctx'))))))

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
      (is (= 0 (:anchor-offset (:window ctx')))))))
