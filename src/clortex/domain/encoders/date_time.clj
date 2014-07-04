(ns clortex.domain.encoders.date-time
"
## [Pre-alpha] Standard Encoders

The Cortical Learning Algorithm consumes data encoded as **Sparse Distributed
Representations** (SDRs), which are arrays or matrices of binary digits (bits).
The functions which convert values into SDRs are `clortex` **encoders**.

**TODO**: Factor out encoder functions. Use Graph or protocols?
"
  (:require [clortex.protocols :refer :all]
            [clortex.utils.hash :refer [sha1 mod-2]]
            [clortex.domain.encoders.hash :as h]))



(defn date-encoder
  "constructs functions to encode values using a hash function"
  [& {:keys [bits on] :or {bits 127 on 21}}]
  (let [truthy #(if % true false)
        encoders (vec (map #(h/hash-on?-fn % bits on) (range bits)))
        encode-all (fn [s] (let [hs (h/hash-bits s bits on)] (vec (map #(vec (list % (truthy (hs %))))
                                                                     (range bits)))))
        encode #(hash-bits % bits on)]
    {:encoders encoders
     :encode-all encode-all
     :encode encode}))
