(ns clortex.domain.sensors.core-test
  (:use midje.sweet)
  (:use clortex.domain.sensors.core))

[[:chapter {:title "Encoders"}]]

"Encoders are very important in `clortex`. Encoders turn real-world data into a form which `clortex` 
can understand - **Sparse Distrubuted Representations** (or *SDRs*). Encoders for the human brain include
retinal cells (or groups of them), as well as cells in the ear, skin, tongue and nose.

"

[[:section {:title "Simple Scalar Encoder"}]]

"Encoders convert input data values into bit-array representations (compatible with Sparse Distributed Representations).
The simplest encoder converts a scalar value into a bit-array as seen below. We'll set up a very small scalar encoder
with 4 bits on out of 12, so we can see how it works."

[[{:title "a very simple scalar encoder" :tag "simple-scalar-encoder"}]]
(comment 
(def enc (scalar-encoder :bits 12 :on 4)) ; uses default params min 0.0, max 100.0
)
