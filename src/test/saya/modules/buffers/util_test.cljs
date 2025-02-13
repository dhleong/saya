(ns saya.modules.buffers.util-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [saya.modules.buffers.util :refer [update-cursor]]
   [saya.modules.input.test-helpers :refer [str->buffer]]))

(deftest update-cursor-test
  (testing "Cross lines"
    (is (= {:row 1 :col 0} (let [buffer (str->buffer "For the hono|r
                                                      of Grayskull!")]
                             (update-cursor buffer (:cursor buffer) inc))))

    (is (= {:row 0 :col 12} (let [buffer (str->buffer "For the honor
                                                       |of Grayskull!")]
                              (update-cursor buffer (:cursor buffer) dec)))))

  (testing "Stay at the end"
    (is (= {:row 1 :col 12} (let [buffer (str->buffer "For the honor
                                                       of Grayskull|!")]
                              (update-cursor buffer (:cursor buffer) inc))))))

