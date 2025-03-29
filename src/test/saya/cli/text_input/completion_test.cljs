(ns saya.cli.text-input.completion-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [saya.cli.text-input.completion :refer [next-candidate]]))

(deftest completion-test
  (testing "Complete single candidate with no state"
    (is (=
         {:cursor 7
          :completion {:word "c"
                       :candidates ["connect"]
                       :result "connect"
                       :index 0}}
         (next-candidate {:cursor 1}
                         (fnil inc -1)
                         "c"
                         {:word "c"
                          :candidates ["connect"]}))))

  (testing "Reset to original after single candidate"
    (is (=
         {:cursor 1
          :completion {:word "c"
                       :candidates ["connect"]
                       :result "c"
                       :index nil}}
         (next-candidate {:cursor 7
                          :completion {:word "c"
                                       :candidates ["connect"]
                                       :result "connect"
                                       :index 0}}
                         (fnil inc -1)
                         "connect"
                         {:word "c"
                          :candidates ["connect"]}))))

  (testing "Ensure cycling works when restarting a continued completion"
    ; EG: co<tab><s-tab>n<tab><s-tab> -> `con`
    (is (=
         {:cursor 7
          :completion {:word "con"
                       :candidates ["connect"]
                       :result "connect"
                       :index 0}}
         (next-candidate {:cursor 7
                          :completion {:word "co"
                                       :candidates ["connect"]
                                       :result "connect"
                                       :index nil}}
                         (fnil inc -1)
                         "con"
                         {:word "con"
                          :candidates ["connect"]}))))

  (testing "Reset state correctly with new completion"
    (is (=
         {:cursor 9
          :completion {:word "h"
                       :candidates ["honor"]
                       :result "for honor"
                       :index 0}}
         (next-candidate {:cursor 5
                          :completion {:word "f"
                                       :candidates ["for"]
                                       :result "for"
                                       :index 0}}
                         (fnil inc -1)
                         "for h"
                         {:word "h"
                          :candidates ["honor"]})))))
