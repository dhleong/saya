(ns saya.modules.window.wrapping-test
  (:require
   ["ink" :as k]
   [archetype.util :refer [>evt]]
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rft]
   [re-frame.core :as rf]
   [saya.prelude]
   [saya.util.ink-testing-utils :refer [render->string]]
   [saya.views :as views]))

(when-not js/process.env.CI
  (deftest basic-render-test
    (testing "Basic rendering"
      (is (= "Hi" (render->string [:> k/Text "Hi"]))))

    (testing "App rendering"
      (rft/run-test-sync
       (rf/clear-subscription-cache!)
       (>evt [:saya.events/initialize-db])
       (is (= "   Welcome to saya"
              (render->string
               {:width 21
                :height 1}
               [views/main]))))))

  (defn- initialize-buffer [& lines]
    (rf/clear-subscription-cache!)
    (>evt [:saya.events/initialize-db])
    (>evt [:submit-raw-command "enew"])
    (doseq [line lines]
      (>evt [:saya.modules.buffers.events/new-line {:id 0}])
      (>evt [:saya.modules.buffers.events/append-text
             {:id 0 :ansi line}])))

  (deftest buffer-line-rendering
    (testing "Basic buffer line rendering with ansi"
      (rft/run-test-sync
       (initialize-buffer "\u001B[32mHi there")
       (is (= "\u001B[32mHi there\u001b[39m"
              (render->string
               {:width 21
                :height 1
                :ansi? true}
               [views/main])))))))
