(ns clortex.domain.encoders.core
  (:require [clortex.domain.encoders.protocols]
	[clortex.utils.hash :refer [sha1 mod-2]]))

(defn within+-
	"returns true if x is within plus or minus window of centre"
	[x centre window]
	(and (> x (- centre window))
	     (<= x (+ centre window))))

(defn low-bit? [bit on] (< bit on))
(defn high-bit? [bit bits on] (<= bits (+ bit on)))

(defn scalar-on?-fn
  "creates a bit encoder for the scalar encoder" 
  [i min' max' bits on gap half-on w] 
  (let [low-bit-off? (+ min' (* i gap))
        high-bit-off? (- max' (* (- bits i) gap))
        centre (+ min' (* (- i half-on) gap))]
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
      encode (fn [x] (set (vec (map first (filter second (encode-all x))))))]
	{:encoders encoders
	 :encode-all encode-all
	 :encode encode}))

(defn hash-bits
	"makes a list of on-bits using the SHA1 hash of a string"
	[s len on]
	(loop [coll (sorted-set) bits (cycle (sha1 s)) bit 0] 
	  (let [step (first bits) bit (mod-2 (+ bit step) len)] 
	    (if (= on (count coll)) 
	        coll 
	        (recur (conj coll bit) (next bits) bit)))))

(defn hash-on?-fn
	[i bits on]
	(fn [s] (if ((hash-bits s bits on) i) true false)))	
	
(defn hash-encoder
	"constructs functions to encode scalars using a hash function"
	[& {:keys [bits on] :or {bits 127 on 21}}]
	(let [truthy #(if % true false)
		encoders (vec (map #(hash-on?-fn % bits on) (range bits)))
		encode-all (fn [s] (let [hs (hash-bits s bits on)] (vec (map #(vec (list % (truthy (hs %)))) 
		  (range bits)))))
		encode #(hash-bits % bits on)]
	{:encoders encoders
	 :encode-all encode-all
	 :encode encode}))
	
(def opf-timestamp-re #"(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):([0-9.]+)")