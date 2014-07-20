(ns clortex.temporal-demo
  (:use midje.sweet)
  (:require [quil.core :as q]
            [clortex.viz.temporal :as v]))


(q/defsketch example
  :title "Temporal Pooling Visualisation"
  :setup v/setup
  :draw v/draw
  :size [800 400]
  :key-typed v/key-press)
