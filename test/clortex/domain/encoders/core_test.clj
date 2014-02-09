(ns clortex.domain.encoders.core-test
  (:use midje.sweet)
  (:use clortex.domain.encoders.core))

[[:chapter {:title "Encoders"}]]

[[:section {:title "Simple Scalar Encoder"}]]

"Encoders convert input data values into bit-array representations (compatible with Sparse Distributed Representations).
The simplest encoder converts a scalar value into a bit-array as seen below."

(def my-encoder (scalar-encoder :bits 12 :on 4)) ; uses default params min 0.0, max 100.0
(def my-encode-fn (:encode my-encoder))
(def my-encode-all-fn (:encode-all my-encoder))

(fact
(my-encode-fn 0) => #{0 1 2 3}
(my-encode-fn 100) => #{8 9 10 11}
(my-encode-all-fn 0) => [[0 true] [1 true] [2 true] [3 true] 
                         [4 false] [5 false] [6 false] [7 false] 
                         [8 false] [9 false] [10 false] [11 false]]
)

(def my-encoder (scalar-encoder)) ; uses default params 127 bits, 21 on, min 0.0, max 100.0
(def my-encode-fn (:encode my-encoder))

(fact
(my-encode-fn 0) => #{0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20}
(count (my-encode-fn 0)) => 21
(my-encode-fn 50) => #{53 54 55 56 57 58 59 60 61 62 63 64 65 66 67 68 69 70 71 72 73}
(count (my-encode-fn 50)) => 21
(my-encode-fn 100) => #{106 107 108 109 110 111 112 113 114 115 116 117 118 119 120 121 122 123 124 125 126}
)

(fact 
(within+- 10 8 1.5) => false
(within+- 10 8 3) => true
)



