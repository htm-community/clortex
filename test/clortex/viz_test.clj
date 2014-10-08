(ns clortex.viz-test
  (:use midje.sweet)
  (:require [quil.core :as q]
            [clortex.viz.core :as v]))

#_(q/defsketch example
  :title "Clortex Visualisation"
  :setup v/setup
  :draw v/draw
  :size [1600 800]
  :key-typed v/key-press)

