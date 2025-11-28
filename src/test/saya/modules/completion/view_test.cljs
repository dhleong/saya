(ns saya.modules.completion.view-test
  (:require
   ["ink" :as k]
   [archetype.util :refer [>evt]]
   [cljs.test :refer-macros [deftest is testing]]
   [clojure.string :as str]
   [day8.re-frame.test :as rft]
   [re-frame.core :as rf]
   [saya.cli.fullscreen :refer [fullscreen-box]]
   [saya.modules.completion.events :as events]
   [saya.modules.completion.view :refer [completion-menu]]
   [saya.modules.ui.cursor :refer [cursor]]
   [saya.util.ink-testing-utils :refer [render->string]]))

(defn- initialize-buffer []
  (rf/clear-subscription-cache!)
  (>evt [:saya.events/initialize-db])
  (>evt [:saya.events/set-global-cursor {:x 2 :y 0}])
  (>evt [::events/set-bufnr 0])
  (>evt [::events/start {:bufnr 0 :word-to-complete "b"}])
  (>evt [::events/on-candidates {:bufnr 0 :candidates ["burrito"
                                                       "best"
                                                       "buffet"]}]))

(deftest absolute-position-test
  (testing "Verify absolute positioning works"
    ; Really, this is a smoke test for CI
    (rft/run-test-sync
     (is (= "\n\n     burrito\n\n"
            (render->string
             {:width 20
              :height 5
              :ansi? false}
             [fullscreen-box {}
              [:> k/Box {:position :absolute
                         :left 5
                         :top 2}
               [:> k/Text "burrito"]]]))))))

(deftest completion-view-test
  (testing "Completion menu has filled background"
    (rft/run-test-sync
     (initialize-buffer)
     ; NOTE: We're omitting checking the ansi coloring here for
     ; now for simplicity, but note that if we *weren't* filling in
     ; the color of the completion, we would not have the spaces after,
     ; (as on the "input" line)
     (is (= (->> [" b"
                  " burrito "
                  " best    "
                  " buffet  "
                  ""]
                 (str/join "\n"))
            (render->string
             {:width 80
              :height 5
              :ansi? false}
             [:<>
              [fullscreen-box {:flex-direction :column}
               [:> k/Box
                [:> k/Text " b"]
                [cursor :pipe]]]
              [completion-menu]]))))))

