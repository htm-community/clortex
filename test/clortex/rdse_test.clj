(ns clortex.rdse-test
  (:use midje.sweet)
  (:require [clortex.domain.encoders.core :refer :all]
	[clortex.domain.encoders.rdse :refer :all]
	[clortex.utils.math :refer :all]
	[clojure.set :refer [difference]]))

[[:chapter {:title "Background"}]]
"
Chetan Surpur gave a great talk on a new version of the Scalar Encoder for NuPIC 
([video](http://www.youtube.com/watch?v=_q5W2Ov6C9E)). This document explores the 
implications of the new design and makes some recommendations about further improvements.

A **Scalar Encoder** is a function which converts a scalar value `v` into a bit map `sdr` 
with a given number `bits` of possible bits, of which `width` bits are 1 and the rest are 0.
One important property of the SDRs is that values close together share many more bits than
values further apart.

Here's a simple example:
"
(facts
(def enc (scalar-encoder :bits 12 :on 4 :min' 1 :max' 12))
(def to-bitstring (:encode-to-bitstring enc))

(vec (map to-bitstring (range 1 13)))
=> ["111100000000" 
      "011110000000" 
      "001111000000" 
      "000111100000" 
      "000111100000" 
      "000011110000" 
      "000001111000" 
      "000000111100" 
      "000000111100" 
      "000000011110" 
      "000000001111" 
      "000000001111"]
)

[[:section {:title "The Current Scalar Encoder"}]]

"
The current scalar encoder (see example above) represents each scalar using a sliding window of `on`
1's. The current scalar encoder has the benefit of being instantly understandable (once you see an 
example like the one in the first section) and visually decodable by the user. It does, however, have 
a number of limitations.

The encoder is given a `min` and `max` when defined and it clamps its output when given values outside
its initial range.  
"
(fact 
(to-bitstring -20) => "111100000000"
(to-bitstring 20) => "000000001111"
)
"
This means that the above two values come to represent many values, and the region receiving the encoding
will not be able to discriminate between these values.

The second limitation is that the encoding is very inefficient in its use of available bits. 
The number of possible `on`-bits out of `bits` bits is given by the `binomial` coefficient:
"
(fact (binomial 12 4) => 495)
"which is significantly larger than the number of distinct SDRs produced by the `scalar-encoder`:"
(fact (count (set (map to-bitstring (range -100 100)))) => 9)
"This encoder is only using 1.8% of the SDRs available. Perhaps this is an extreme example because of
the very small number of bits. Let's check the numbers for a more typical encoder:"
(fact 
(def enc (scalar-encoder :bits 512 :on 21 :min' 1 :max' 512))
(def to-bitstring (:encode-to-bitstring enc))
(str (binomial 512 21)) => "10133758886507113849867996785041062400"
(count (set (map to-bitstring (range -100 1000)))) => 492
)
"Oh dear."

[[:section {:title "The Random Distributed Scalar Encoder"}]]

"Chetan's talk explains a new approach. The idea is to use a set of 'buckets' to represent a set of
intervals in the encoders range, and to encode each one using a randomly chosen set of `on` bits.
The property of semantic closeness is achieved by only changing one bit at a time when choosing the 
bits for the next bucket along.

Let's implement an RDSE. We'll need a utility function so we always get the same SDRs.
"
(fact
(def randomer (random-fn-with-seed 123456)) ; returns the same sequence of random numbers
(randomer 10) => 3
(randomer 10) => 7
)
"
First, 
"
(def encoder (random-sdr-encoder-1 :diameter 1.0 :bits 12 :on 4))
(def buckets (:buckets encoder))
(def to-bitstring (:encode-to-bitstring! encoder))
(fact
(find-bucket! 10 buckets) => nil
(add-bucket! 10 buckets) 
(find-bucket! 10 buckets) => {:bottom 9.5, :counter 1, :index 0, :read 0, :sdr [0 1 2 3], :top 10.5}

(find-bucket! 20 buckets) => nil
(add-bucket! 20 buckets)
(find-bucket! 20 buckets) => {:bottom 19.5, :counter 1, :index 10, :read 0, :sdr [7 9 10 11], :top 20.5}

(find-bucket! 0 buckets) => nil
(add-bucket! 0 buckets)
(find-bucket! 0 buckets) => {:bottom -0.5, :counter 1, :index 20, :read 0, :sdr [0 1 2 10], :top 0.5}

(to-bitstring 1) => "111000000100"

(vec (map to-bitstring (range -5 22))) 
=> ["111010000000" 
      "111000100000" 
      "111100000000" 
      "111000000001" 
      "111000100000" 
      "111000000010" 
      "111000000100" 
      "111100000000" 
      "111000000001" "111001000000" "111000001000" "111010000000" "111000010000" 
"111010000000" "111000001000" "111100000000" "011100010000" "001100010001" 
"000100010011" "000010010011" "001000010011" "100000010011" "000000110011" 
"001000010011" "100000010011" "000000010111" "000000001111"]


(def encoder (random-sdr-encoder-1 :diameter 1.0 :bits 512 :on 21))
(def buckets (:buckets encoder))
(def to-bitstring (:encode-to-bitstring! encoder))
(str (binomial 512 21)) => "10133758886507113849867996785041062400"
(count (set (map to-bitstring (range -1000 1000)))) => 1544
)
#_(fact 
)

