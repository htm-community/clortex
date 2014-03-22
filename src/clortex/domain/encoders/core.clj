(ns clortex.domain.encoders.core
"
## [Pre-alpha] Standard Encoders

The Cortical Learning Algorithm consumes data encoded as **Sparse Distributed Representations** (SDRs), which
are arrays or matrices of binary digits (bits). The functions which convert values into SDRs are `clortex`
**encoders**.

**TODO**: Factor out encoder functions. Use Graph or protocols?
"
  (:require [clortex.domain.encoders.protocols]
	[clortex.utils.hash :refer [sha1 mod-2]]))

(defn within+-
	"`true` if `x` is within plus or minus `window` of `centre`"
	[x centre window]
	(and (> x (- centre window))
	     (<= x (+ centre window))))

(defn low-bit? "`true` if `bit` is in the first `on` bits" [bit on] (< bit on))
(defn high-bit? "`true` if `bit` is in the last `on` bits (of `bits`)" [bit bits on] (<= bits (+ bit on)))

(defn scalar-on?-fn
  "creates a bit encoder fn for the scalar encoder. the first `on` bits and the last `on` bits
   respond to inputs at the bottom and top of the encoder's range. other bits respond to values
   within a window of their centres."
  [i min' max' bits on gap half-on w]
  (let [low-bit-off? (+ min' (* i gap))
        high-bit-off? (- max' (* (- bits i) gap))
        centre (+ min' (* (- i half-on) gap))]
	#_(println (str "i " i "\tlow-bit? " (low-bit? i on) "\thigh-bit? " (high-bit? i bits on) "\tcentre " centre))
    (if (low-bit? i on)
        #(<= % low-bit-off?)
        (if (high-bit? i bits on)
            #(> % high-bit-off?)
            #(within+- % centre (/ w 1.0))))))

(defn scalar-encoder
	"constructs functions to encode scalars using a clamped linear sliding window"
	[& {:keys [min' max' bits on] :or {min' 0.0 max' 100.0 bits 127 on 21}}]
	(let [
      gap (/ (- max' min') (- bits on))
      half-on (/ on 2)
      w (* gap half-on)
	  encoders (vec (map #(scalar-on?-fn % min' max' bits on gap half-on w) (range bits)))
	  encode-all (fn [x] (vec (map #(vec (list % ((encoders %) x))) (range bits))))
      encode (fn [x] (set (vec (map first (filter second (encode-all x))))))
      encode-to-bitstring (fn [x] (apply str (vec (map #(if ((encoders %) x) 1 0) (range bits)))))
 #_(fn [x] (str (doall (map #(if (second %) 1 0) (encode-all x)))))]
	{:encoders encoders
	 :encode-all encode-all
	 :encode encode
	 :encode-to-bitstring encode-to-bitstring}))

(defn hash-bits
	"makes a list of on-bits using the SHA1 hash of a string"
	[s len on]
	(loop [coll (sorted-set) bits (cycle (sha1 s)) bit 0]
	  (let [step (first bits)              ; skip step bits in the set
            bit (mod-2 (+ bit step) len)]  ; wrap around the set
	    (if (= on (count coll))            ; enough bits?
	        coll
	        (recur (conj coll bit) (next bits) bit)))))

(defn hash-on?-fn
	"converts non-nil/nil to true/false"
	[i bits on]
	(fn [s] (if ((hash-bits s bits on) i) true false)))

(defn hash-encoder
	"constructs functions to encode values using a hash function"
	[& {:keys [bits on] :or {bits 127 on 21}}]
	(let [truthy #(if % true false)
		encoders (vec (map #(hash-on?-fn % bits on) (range bits)))
		encode-all (fn [s] (let [hs (hash-bits s bits on)] (vec (map #(vec (list % (truthy (hs %))))
		  (range bits)))))
		encode #(hash-bits % bits on)]
	{:encoders encoders
	 :encode-all encode-all
	 :encode encode}))

(defn date-encoder
	"constructs functions to encode values using a hash function"
	[& {:keys [bits on] :or {bits 127 on 21}}]
	(let [truthy #(if % true false)
		encoders (vec (map #(hash-on?-fn % bits on) (range bits)))
		encode-all (fn [s] (let [hs (hash-bits s bits on)] (vec (map #(vec (list % (truthy (hs %))))
		  (range bits)))))
		encode #(hash-bits % bits on)]
	{:encoders encoders
	 :encode-all encode-all
	 :encode encode}))
