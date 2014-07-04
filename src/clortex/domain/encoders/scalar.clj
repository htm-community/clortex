(ns clortex.domain.encoders.scalar
"
## [Pre-alpha] Standard Encoders

The Cortical Learning Algorithm consumes data encoded as **Sparse Distributed
Representations** (SDRs), which are arrays or matrices of binary digits (bits).
The functions which convert values into SDRs are `clortex` **encoders**.

**TODO**: Factor out encoder functions. Use Graph or protocols?
"
  (:require [clortex.protocols :refer :all]
            [clortex.utils.hash :refer [sha1 mod-2]]))

(defn within+-
	"`true` if `x` is within plus or minus `window` of `centre`"
	[x centre window]
	(and (> x (- centre window))
	     (<= x (+ centre window))))

(defn low-bit? "`true` if `bit` is in the first `on` bits"
  [bit on] (< bit on))
(defn high-bit? "`true` if `bit` is in the last `on` bits (of `bits`)"
  [bit bits on] (<= bits (+ bit on)))

(defn scalar-on?-fn
  "creates a bit encoder fn for the scalar encoder. the first `on` bits and
   the last `on` bits respond to inputs at the bottom and top of the encoder's
   range. other bits respond to values within a window of their centres."
  [i min' max' bits on gap half-on w]
  (let [low-bit-off? (+ min' (* i gap))
        high-bit-off? (- max' (* (- bits i) gap))
        centre (+ min' (* (- i half-on) gap))]
    (if (low-bit? i on)
        #(<= % low-bit-off?)
        (if (high-bit? i bits on)
            #(> % high-bit-off?)
            #(within+- % centre (/ w 1.0))))))

(defrecord ScalarEncoder [field-name bits on-bits minimum maximum encoders encode]
  CLAEncoder)

(defn scalar-encoder
  "constructs functions to encode scalars using a clamped linear sliding window"
  [& {:keys [minimum maximum bits on] :or {minimum 0.0 maximum 100.0 bits 127 on 21}}]
  (let [gap (/ (- maximum minimum) (- bits on))
        half-on (/ on 2)
        w (* gap half-on)
        encoders (mapv #(scalar-on?-fn % minimum maximum bits on gap half-on w)
                       (range bits))
        encode-all (fn [x] (mapv #(vector % ((encoders %) x)) (range bits)))
        encode (fn [x] (set (mapv first (filter second (encode-all x)))))
        encode-to-bitstring (fn [x] (apply str (mapv #(if ((encoders %) x) 1 0) (range bits))))]
    {:encoders encoders
     :encode-all encode-all
     :encode encode
     :encode-to-bitstring encode-to-bitstring}))


