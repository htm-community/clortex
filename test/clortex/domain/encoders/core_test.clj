(ns clortex.domain.encoders.core-test
  (:use midje.sweet)
  (:use clortex.domain.encoders.core))

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

"`scalar-encoder` returns a map of functions which can be used in various parts of the encoding
of data. We'll define those functions by pulling them out of the map:"
[[{:tag "simple-functions" :title "pulling out the functions"}]]
(def encode (:encode enc))
(def encode-all (:encode-all enc))

"Let's check that the bottom and top values give the bottom and top SDRs:"
(fact
(encode 0) => #{0 1 2 3}
(encode 100) => #{8 9 10 11}
(encode-all 0) =>
  [[0 true]  [1 true]  [ 2 true]  [ 3 true] 
   [4 false] [5 false] [ 6 false] [ 7 false] 
   [8 false] [9 false] [10 false] [11 false]]
)

"`scalar-encoder` defaults to NuPIC's scalar encoder parameters:"

(def enc (scalar-encoder)) ; uses default params 127 bits, 21 on, min 0.0, max 100.0
(def encode (:encode enc))

(fact
(encode 0) => 
#{ 0  1  2  3  4  5  6
     7  8  9 10 11 12 13 
    14 15 16 17 18 19 20}
(count (encode 0)) => 21
(encode 50) => 
#{53 54 55 56 57 58 59 
    60 61 62 63 64 65 66
    67 68 69 70 71 72 73}
(count (encode 50)) => 21
(encode 100) => 
#{106 107 108 109 110 111 112 
    113 114 115 116 117 118 119 
    120 121 122 123 124 125 126}
)

(fact 
(within+- 10 8 1.5) => false
(within+- 10 8 3) => true
)



