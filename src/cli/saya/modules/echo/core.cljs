(ns saya.modules.echo.core
  (:require
   [archetype.util :refer [>evt]]))

(defn echo [& parts]
  (>evt (into [:echo] parts)))

(defn echo-fx [& parts]
  [:dispatch (into [:echo] parts)])
