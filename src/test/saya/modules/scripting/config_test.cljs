(ns saya.modules.scripting.config-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [saya.modules.scripting.config :refer [format-user-keymaps]]))

(defn- test->f [_connr {:keys [send]}]
  (keyword send))

(deftest format-user-keymaps-test
  (testing "Handle single string modes in various places"
    (is (= {:normal {["H"] :adora}}
           (format-user-keymaps
            0
            test->f
            [["H" {:send "adora"}]])))

    ; :modes metadata
    (is (= {:insert {["H"] :adora}}
           (format-user-keymaps
            0
            test->f
            [^{:modes "i"} ["H" {:send "adora"}]])))

    ; :mode metadata
    (is (= {:insert {["H"] :adora}}
           (format-user-keymaps
            0
            test->f
            [^{:mode "i"} ["H" {:send "adora"}]])))

    ; :modes in the rhs
    (is (= {:insert {["H"] :adora}}
           (format-user-keymaps
            0
            test->f
            [["H" {:send "adora"
                   :modes "i"}]])))

    ; :modes in opts position
    (is (= {:insert {["H"] :adora}}
           (format-user-keymaps
            0
            test->f
            [["H" {:send "adora"} {:modes "i"}]]))))

  (testing "Handle multi-mode strings"
    (is (= {:normal {["H"] :adora}
            :insert {["H"] :adora}}
           (format-user-keymaps
            0
            test->f
            [["H" {:send "adora"
                   :modes "ni"}]]))))

  (testing "Handle keywords in various forms"
    (is (= {:insert {["H"] :adora}}
           (format-user-keymaps
            0
            test->f
            [["H" {:send "adora"
                   :mode :insert}]])))

    (is (= {:insert {["H"] :adora}}
           (format-user-keymaps
            0
            test->f
            [^:insert ["H" {:send "adora"}]])))))

