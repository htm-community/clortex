(ns clortex.domain.encoders.compound
"
## [Pre-alpha] Compound Encoders

"
  (:require [clortex.protocols :refer :all]))


(defn encoding-with-offset
  [sdr bits offset combined]
    [(+ offset bits) (into combined (map (partial + offset) sdr))])


(defn offset-encoding
  [t enc [offset combined]]
  ;(println "encoder:\n" enc)
  (let [sdr ((:encode enc) t)]
    (encoding-with-offset sdr (:bits enc) offset combined)))

(defn map-encoding
  [[v enc] accum]
  ;(println "v:" v "accum:\n" accum)
  (offset-encoding v enc accum))

(defn combined-enc
  [t encs]
  (second (reduce #(offset-encoding t %2 %1) [0 #{}] encs)))

(defn compound-enc
  [xs encs]
  (second (reduce #(map-encoding %2 %1) [0 #{}] (map vector xs encs))))


#_(defn compound-encoder
  "constructs functions to encode a vector of encoders"
  [encoders]
  (let [bit-encoders (reduce #(scalar-on?-fn % minimum maximum bits on gap half-on w)
                       (range bits))
        encode-all (fn [x] (mapv #(vector % ((encoders %) x)) (range bits)))
        encode (fn [x] (set (mapv first (filter second (encode-all x)))))
        encode-to-bitstring (fn [x] (apply str (mapv #(if ((encoders %) x) 1 0) (range bits))))]
    {:encoders bit-encoders
     :encode-all encode-all
     :encode encode
     :encode-to-bitstring encode-to-bitstring}))
