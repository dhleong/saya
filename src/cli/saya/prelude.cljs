(ns saya.prelude
  (:require
   [reagent.core :as r]
   [saya.cofx]
   [saya.events]
   [saya.fx]
   [saya.subs]))

(defonce ^:private functional-compiler (r/create-compiler
                                        {:function-components true}))
(r/set-default-compiler! functional-compiler)
