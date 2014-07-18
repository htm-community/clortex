(ns clortex.domain.encoders.hash
"
## [Pre-alpha] Standard Encoders

The Cortical Learning Algorithm consumes data encoded as **Sparse Distributed
Representations** (SDRs), which are arrays or matrices of binary digits (bits).
The functions which convert values into SDRs are `clortex` **encoders**.

**TODO**: Factor out encoder functions. Use Graph or protocols?
"
  (:require [clortex.protocols :refer :all]
            [clortex.utils.hash :refer [sha1 mod-2]]))

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
     :encode encode
     :bits bits
     :on on}))
