(ns saya.ci
  (:require
   [saya.cli.text-input.completion-test]
   [saya.modules.buffers.events-test]
   [saya.modules.input.helpers-test]
   [cljs.test :as test]))

(defmethod test/report [::test/default :end-run-tests]
  [{:keys [fail error]}]
  (js/process.exit (min (+ fail error) 1)))

(test/run-all-tests #".*-test$")
