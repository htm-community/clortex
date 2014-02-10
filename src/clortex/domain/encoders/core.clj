(ns clortex.domain.encoders.core
  #_(:require [clortex.domain.sensor.encoders.core :as enc :refer :all]))

(defn within+-
	"returns true if x is within plus or minus window of centre"
	[x centre window]
	(and (> x (- centre window))
	     (<= x (+ centre window))))

(defn low-bit? [bit on] (< bit on))
(defn high-bit? [bit bits on] (<= bits (+ bit on)))

(defn on?-fn
  "creates a bit encoder for the scalar encoder" 
  [i min' max' bits on gap half-on w] 
  (let [low-bit-off? (+ min' (* i gap))
        high-bit-off? (- max' (* (- bits i) gap))
        centre (+ min' (* (- i half-on) gap))]
    #_(do
	(println (str "bit " i " low? " (low-bit? i on) " high? " (high-bit? i bits on)))
    (println (str "bit " i " low: " low-bit-off? " high: " high-bit-off?))
    (println (str "bit " i " centre: " centre " w: " w)))
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
	  encoders (vec (map #(on?-fn % min' max' bits on gap half-on w) (range bits)))
	  encode-all (fn [x] (vec (map #(vec (list % ((encoders %) x))) (range bits))))
      encode (fn [x] (set (vec (map first (filter second (encode-all x))))))]
    (println (str "gap " gap " half-on: " half-on))
	{:encoders encoders
	 :encode-all encode-all
	 :encode encode}))
	
