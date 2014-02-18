(ns clortex.domain.patch.patch-test
  (:use midje.sweet)
  (:require [clortex.domain.patch.core :refer :all]))

[[:chapter {:title "Introduction"}]]

"The `patch` in `clortex` is the core component of the Cortical Learning Algorithm and thus of 
Hierarchical Temporal Memory. The theory talks about Regions (as does NuPIC) and Layers, but `clortex`
uses `patch` to stand for either a single-layer Region or a Layer in a multi-layer Region. What defines
a `patch` is that it contains a set of neurons and it communicates with other components in `clortex` 
(such as sensors, classifiers and other patches in a Region or hierarchy).
"
[[:section {:title "Design"}]]

"

Each `patch` has a **feedforward** vector of input lines, on which incoming signals impinge. Neurons in
the patch are connected via their proximal dendrites to the signals on this vector. The `patch` also has
a vector of **activation** and **prediction** output lines which show the current state of the neurons. 

The state of a patch is 
"