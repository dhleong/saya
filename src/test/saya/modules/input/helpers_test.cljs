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
             (:cursor (:buffer ctx')))))))
