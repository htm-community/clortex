(ns clortex.viz-test
  (:use midje.sweet)
  (:require [quil.core :as q]
            [clortex.viz.core :as v]))

(q/defsketch example
  :title "Clortex Visualisation"    ;; Set the title of the sketch
  :setup v/setup                      ;; Specify the setup fn
  :draw v/draw                        ;; Specify the draw fn
  :size [1200 600])                  ;; You struggle to beat the golden ratio

