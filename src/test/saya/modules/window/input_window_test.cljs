(ns saya.modules.window.input-window-test
  (:require
   [archetype.util :refer [>evt]]
   [cljs.test :refer-macros [deftest is testing]]
   [day8.re-frame.test :as rft]
   [re-frame.core :as rf]
   [re-frame.db :as rfdb]
   [saya.modules.input.core :as input]
   [saya.modules.kodachi.events :as kodachi-events]
   [saya.modules.window.events :as window-events]
   [saya.util.ink-testing-utils :refer [create-renderer input! render->vec]]
   [saya.views :as views]))

(defn receive-text [text]
  (>evt [::kodachi-events/on-message {:connection_id 0
                                      :type "ExternalUI"
                                      :data {:type "Text"
                                             :ansi text}}]))
(defn receive-newline []
  (>evt [::kodachi-events/on-message {:connection_id 0
                                      :type "ExternalUI"
                                      :data {:type "NewLine"}}]))

(defn receive-full-line [line]
  (receive-text (str line "\n"))
  (receive-newline))

(defn- initialize-buffer [& {:keys [input-state initial-mode output
                                    prompt]}]
  (rf/clear-subscription-cache!)
  (>evt [:saya.events/initialize-db])
  (>evt [::kodachi-events/connecting {:uri "burrito.stand:9001"
                                      :connection-id 0}])
  (doseq [line output]
    (receive-full-line line))

  (when prompt
    (receive-text prompt))

  (when input-state
    (when (= :insert initial-mode)
      (>evt [::input/on-key "i"]))
    (>evt [::window-events/set-input-text {:connr 0
                                           :text input-state}])

    ; Gross:
    (swap! rfdb/app-db assoc-in [:buffers [:conn/input 0] :cursor :col] (count input-state))))

(deftest connection-input-window-persistence-test
  (testing "Input window state should persist across receiving new output"
    (rft/run-test-sync
     (initialize-buffer
      :input-state "a"
      :initial-mode :insert
      :output ["Choose from the menu:"
               " - Alidocious"
               " - Alpastor"]
      :prompt "> ")

     (let [renderer (create-renderer
                     {:height 8
                      :ansi? false}
                     [views/main])]
       (is (= ["Connecting to burrito.stand:9001..."
               "Choose from the menu:"
               " - Alidocious"
               " - Alpastor"
               "> a"
               ""
               ""]
              (-> (render->vec renderer)
                  (drop-last)))
           "Pre-input state not matched")

       (input! renderer "l")

       (is (= ["Connecting to burrito.stand:9001..."
               "Choose from the menu:"
               " - Alidocious"
               " - Alpastor"
               "> al"
               ""
               ""]
              (-> (render->vec renderer)
                  (drop-last)))
           "Initial input state not matched")

       (receive-full-line " - Alakazam")

       (is (= ["Connecting to burrito.stand:9001..."
               "Choose from the menu:"
               " - Alidocious"
               " - Alpastor"
               ">  - Alakazam"
               "al"
               ""]
              (-> (render->vec (create-renderer
                                {:height 8
                                 :ansi? false}
                                [views/main]))
                  (drop-last)))
           "State not maintained")))))

