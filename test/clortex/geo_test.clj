(ns clortex.geo-test
  (:use midje.sweet)
  (:require [clortex.domain.encoders.core :refer :all]
            [clortex.domain.encoders.scalar :as s]
            [clortex.domain.encoders.rdse :as r]
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
(def enc (s/scalar-encoder :bits 12 :on 4 :minimum 1 :maximum 12))
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
(def enc (s/scalar-encoder :bits 512 :on 21 :minimum 1 :maximum 512))
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

[[:subsection {:title "Basic Random Distributed Scalar Encoder - Implementation"}]]
"
The following is a basic RDSE implemented in Clojure. It starts off empty, and adds buckets lazily as new
values are encoded. **Please let me know if there is anything unclear in this code!**.
"
[[:file {:src "src/clortex/domain/encoders/rdse.clj"}]]

[[:subsection {:title "Testing the RDSE on 4-of-12-bit encoding"}]]
"Let's re-run the tests for the simple 12 bit encoder, where we had very quickly saturated the
basic `scalar-encoder`."

(fact
(def encoder (r/random-sdr-encoder-1 :diameter 1.0 :bits 12 :on 4))
(def buckets (:buckets encoder))
(def to-bitstring (:encode-to-bitstring! encoder))

(to-bitstring 1) => "111100000000"

(vec (map to-bitstring (range -5 22)))
=> ["000010111000"
       "010010101000"
       "010010001001"
       "011010000001"
       "011001000001"
       "011100000001"
       "111100000000"
       "111000001000"
       "011000001010"
       "011000101000"
       "010000101100"
       "000100101100"
       "000101101000"
       "000101001010"
       "000101010010"
       "010001010010"
       "001001010010"
       "000001110010"
       "000000110110"
       "100000110100"
       "000000111100"
       "010000111000"
       "001000111000"
       "001000011001"
       "101000010001"
       "100000010011"
       "100000010101"]

(count (set (map to-bitstring (range -500 500)))) => 436
)
"
As we can see, this encoder is capable of storing 436 out of 495 passible encodings (note that each
pair of encoded buckets differs only in one bit, so this is pretty good).
"
[[:subsection {:title "A larger encoding: 21-of-128 bits"}]]
"
Let's see how much capacity we can get with a more typical 128 bit encoding (standard 21 bits on).
"
(fact
(def encoder (r/random-sdr-encoder-1 :diameter 1.0 :bits 128 :on 21))
(def buckets (:buckets encoder))
(def to-bitstring (:encode-to-bitstring! encoder))
(str (binomial 512 21)) => "10133758886507113849867996785041062400"
(to-bitstring 0) => "11111111111111111111100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
(to-bitstring 1) => "11111111111111111111000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000"
)
"
We'll put 10000 values into the encoder.
"
(comment
(def n 10000)
(to-bitstring n) => "10000000001010001100000000000100000000000010000011000000101010000000000000011100000001000110010000000000000000010000000000010000"
(println (time (count (set (map to-bitstring (range n))))))
; "Elapsed time: 34176.316 msecs"
(count (set (map to-bitstring (range n)))) => n
)
"
OK, everything's looking good. We could try using a smaller encoding and see if it still
works.
"
(def encoder (r/random-sdr-encoder-1 :diameter 1.0 :bits 64 :on 21))
(def buckets (:buckets encoder))
(def to-bitstring (:encode-to-bitstring! encoder))
(str (binomial 64 21)) => "41107996877935680"
(to-bitstring 0) => "1111111111111111111110000000000000000000000000000000000000000000"
(to-bitstring 1) => "1111011111111111111110000000000000000010000000000000000000000000"

(comment
(def n 1000)
(to-bitstring (dec n)) => "1010100100000010010000100001000000001000000110000011110001111101"
(to-bitstring n) => "1000100100000010010000100001000000001000001110000011110001111101"
(time (count (set (map to-bitstring (range n)))))
; "Elapsed time: 406.187 msecs"
(count (set (map to-bitstring (range n)))) => n
(count (set (map to-bitstring (range 20000)))) => 20000
)

[[:subsection {:title "Encoding is dependent on order of data"}]]
"Observe the following"

(fact
(def to-bitstring (:encode-to-bitstring! (r/random-sdr-encoder-1 :diameter 1.0 :bits 12 :on 4)))
(to-bitstring 0) => "111100000000"
(to-bitstring 1) => "111000000001"
(to-bitstring 2) => "101001000001" ;; first encoding of 2
;; reset
(def to-bitstring (:encode-to-bitstring! (r/random-sdr-encoder-1 :diameter 1.0 :bits 12 :on 4)))
(to-bitstring 0) =>  "111100000000"
(to-bitstring 1) =>  "111000000001"
(to-bitstring -1) => "110101000000"
(to-bitstring 2) =>  "101010000001" ;; a different encoding of 2

)
"The encoding of `2` thus depends on the sequence of buckets already set up when `2` is presented.
The only way to avoid this is to always build the buckets in the same order, regardless of what data
is presented:"

(fact



(def to-bitstring (:encode-to-bitstring! (r/random-sdr-encoder-1 :diameter 1.0 :bits 12 :on 4)))
(defn to-bitstring-pre [x] (r/precalculate x to-bitstring))

(to-bitstring-pre 0) => "001000110100" ;; causes (-10,10) to be encoded in advance
(to-bitstring-pre 1) => "100000110100"
(to-bitstring-pre 2) => "100001110000" ;; first encoding of 2
;; reset
(def to-bitstring (:encode-to-bitstring! (r/random-sdr-encoder-1 :diameter 1.0 :bits 12 :on 4)))
(defn to-bitstring-pre [x] (r/precalculate x to-bitstring))
(to-bitstring-pre 0) =>  "001000110100"
(to-bitstring-pre 1) =>  "100000110100"
(to-bitstring-pre -1) => "001010110000"
(to-bitstring-pre 2) =>  "100001110000" ;; the same encoding of 2

)
[[:section {:title "Conclusions and Further Improvements"}]]
"
It appears from the above tests that indeed the RDSE may be a significant improvement on
the current scalar encoder. It comes a lot closer to exploiting the capacity of SDRs
in the CLA.

We should investigate how we should measure the effectiveness and efficiency of our
choice of encoders and encodings. While the RDSE looks promising, how can we be sure? Perhaps the swarming
algorithms would be able to help us here.

This implementation, as a first draft, seems to be reasonably performant. There may be come tweaks which
will speed it up a good bit (such as removing all the searching code in `new-sdr` and keeping a running cache
of nearby SDRs).

Anyone interested in implementing this in C++ and Python, please do so and let us know how you get on.
"
#_(fact
"
user=> (bench (sort-by :bottom bins))
WARNING: Final GC required 2.157308813480153 % of runtime
Evaluation count : 521760 in 60 samples of 8696 calls.
             Execution time mean : 116.827998 µs
    Execution time std-deviation : 953.353449 ns
   Execution time lower quantile : 114.676491 µs ( 2.5%)
   Execution time upper quantile : 118.637151 µs (97.5%)
                   Overhead used : 2.211817 ns

user=> (bench (vec (take 21 sorted-bins)))
Evaluation count : 32109840 in 60 samples of 535164 calls.
             Execution time mean : 1.900169 µs
    Execution time std-deviation : 21.873260 ns
   Execution time lower quantile : 1.868097 µs ( 2.5%)
   Execution time upper quantile : 1.946967 µs (97.5%)
                   Overhead used : 2.211817 ns

Found 2 outliers in 60 samples (3.3333 %)
	low-severe	 2 (3.3333 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers

user=> (bench (vec (take 21 (reverse sorted-bins))))
Evaluation count : 414600 in 60 samples of 6910 calls.
             Execution time mean : 147.253999 µs
    Execution time std-deviation : 1.433901 µs
   Execution time lower quantile : 145.060885 µs ( 2.5%)
   Execution time upper quantile : 150.177477 µs (97.5%)
                   Overhead used : 2.211817 ns

Found 2 outliers in 60 samples (3.3333 %)
	low-severe	 2 (3.3333 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers

user=> (bench (vec (reverse (drop (- (count sorted-bins) 21) sorted-bins))))
Evaluation count : 1311780 in 60 samples of 21863 calls.
             Execution time mean : 47.140705 µs
    Execution time std-deviation : 1.096611 µs
   Execution time lower quantile : 45.709767 µs ( 2.5%)
   Execution time upper quantile : 49.838158 µs (97.5%)
                   Overhead used : 2.211817 ns

Found 3 outliers in 60 samples (5.0000 %)
	low-severe	 2 (3.3333 %)
	low-mild	 1 (1.6667 %)
 Variance from outliers : 11.0117 % Variance is moderately inflated by outliers

user=> (bench (vec (reduce conj #{} (flatten (map :sdr nearest-buckets)))))
Evaluation count : 184080 in 60 samples of 3068 calls.
             Execution time mean : 325.097234 µs
    Execution time std-deviation : 3.519885 µs
   Execution time lower quantile : 319.770605 µs ( 2.5%)
   Execution time upper quantile : 332.488336 µs (97.5%)
                   Overhead used : 2.211817 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers

user=> (bench (vec (sort (reduce #(clojure.set/union %1 (set (:sdr %2))) #{} nearest-buckets))))
Evaluation count : 442680 in 60 samples of 7378 calls.
             Execution time mean : 138.537716 µs
    Execution time std-deviation : 2.199742 µs
   Execution time lower quantile : 135.486945 µs ( 2.5%)
   Execution time upper quantile : 141.462538 µs (97.5%)
                   Overhead used : 2.211817 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers

user=> (bench (apply str (vec (map #(if (contains? (set sdr) %) 1 0) (range bits)))))
Evaluation count : 129060 in 60 samples of 2151 calls.
             Execution time mean : 469.761371 µs
    Execution time std-deviation : 6.048208 µs
   Execution time lower quantile : 460.879704 µs ( 2.5%)
   Execution time upper quantile : 480.632377 µs (97.5%)
                   Overhead used : 2.211817 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers

(str (binomial 512 16)) => 841141456821158064519401490400
(text-scientific (binomial 512 16)) => 8.41x10^+29


"

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
;(count (set (map to-bitstring (range -1000 1000)))) => 1544
)


