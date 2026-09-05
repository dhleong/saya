(ns saya.modules.input.undo-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [saya.modules.input.test-helpers :refer [has-error?
                                                     has-no-error?
                                                     with-session]]))

(deftest undo-test
  (testing "Forbid redo after a new change"
    (with-session {:buffer "for |the honor"}
      (is (= "for |honor"
             (feed-keys "daw")))
      (is (= "for |the honor"
             (feed-keys "u")))
      (is (= "|the honor"
             (feed-keys "db")))
      (has-no-error?)
      (is (= "|the honor"
             (feed-keys [:ctrl/r])))
      (has-error? #"Already at newest change"))))

