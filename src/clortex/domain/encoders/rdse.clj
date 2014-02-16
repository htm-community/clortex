(ns clortex.domain.encoders.rdse
  (:use midje.sweet)
  (:require [clortex.utils.math :refer :all]
	[clojure.set :refer [difference union]]))

(defn bottom-sorter 
    "sorts buckets by their bottom value"
    [x y]
    (let [c (compare (x :bottom) (y :bottom))]
         (if (not= c 0)
             c
             (compare x y))))

(defn ordered-bins [bins] (sort-by :bottom bins))

(defn find-bucket! 
    "returns the bucket which covers value. updates the 'read' slot of the bucket"
    [^Double value buckets] 
    (when-let [bucket (first (filter #(<= (:bottom %) value (:top %)) (:bins @buckets)))]
        (swap! buckets update-in [:bins (:index bucket) :read] inc)
        bucket))

(defn new-bucket 
    "returns a bucket map given centre, radius and index"
    [^Double value ^Double radius ^long index]
    {:bottom (- value radius) :top (+ value radius) :index index :counter 1 :read 0})

(defn min-distance 
    "used to accumulate the best nearby bucket when searching"
    [acc a-bucket] 
    (let [diff (min (abs-diff (:mine acc) (:bottom a-bucket)) 
                    (:best acc))]
         (if (< diff (:best acc))
             (conj acc {:index (:index a-bucket) :best diff})
             acc)))

(defn sdrs [bins] (reduce conj #{} (map :sdr bins)))
(defn bottom-of-buckets [bins] (reduce min (map :bottom bins)))
(defn top-of-buckets [bins] (reduce max (map :top bins)))
(defn n-bins [buckets] (count (:bins buckets)))

(defn search-starter [bucket] {:index nil :best Integer/MAX_VALUE  :mine (:bottom bucket)})
(defn sdr->bitstring [sdr bits] (apply str (vec (map #(if (contains? (set sdr) %) 1 0) (range bits)))))

(defn new-sdr 
    [bucket buckets]
    (let [bins (:bins buckets)
          ^int on (:on buckets)
          ^int bits (:bits buckets)
          randomer (:randomer buckets)]
         (if (empty? bins) 
             (vec (range on))
             (let [sorted-bins (sort-by :bottom bins)
                   above? (> (:bottom bucket) (:bottom (first sorted-bins)))
                   nearest-buckets 
                       (if above? 
                           (vec (reverse (drop (- (count sorted-bins) on) sorted-bins)))
                           (vec (take on sorted-bins)))
                   nearest-bits (vec (sort (reduce #(union %1 (set (:sdr %2))) #{} nearest-buckets)))
                   previous-sdr (:sdr (first nearest-buckets))
                   previous-sdr (if above? previous-sdr (vec (reverse previous-sdr)))
                   remove-bit (previous-sdr (inc (randomer (dec on))))
                   same-bits (vec (disj (set previous-sdr) remove-bit))
                   free-bits (vec (difference (set (range bits)) (set nearest-bits)))
                   new-bit-pos (randomer (count free-bits))
                   new-bit (free-bits new-bit-pos)
                   new-sdr (vec (sort (conj (set same-bits) new-bit)))]
                new-sdr))))

(defn add-to-buckets! 
    [buckets bucket]
    (let [bits (:bits @buckets)
         sdr (new-sdr bucket @buckets)
         sdr-bucket (conj bucket {:sdr sdr})
         ;bitstring (sdr->bitstring sdr bits)
         ;sdr-bucket (conj bucket {:sdr sdr :bitstring bitstring})
         ]
       (swap! buckets update-in [:bins] conj sdr-bucket)
       sdr-bucket))	

(defn add-bucket! 
    [value buckets]
    (let [diameter (:diameter @buckets)
          radius (/ diameter 2.0)
          mn #(bottom-of-buckets (:bins @buckets))
	      mx #(top-of-buckets (:bins @buckets))]
      (if (empty? (:bins @buckets))
        (let [bucket (new-bucket value radius (n-bins @buckets))]
          (add-to-buckets! buckets bucket))
        (do (while (> value (mx))
	           (add-to-buckets! buckets (new-bucket (+ (mx) radius) radius (n-bins @buckets))))
	        (while (< value (mn)) 
	           (add-to-buckets! buckets (new-bucket (- (mn) radius) radius (n-bins @buckets))))))))

(defn random-sdr-encoder-1
    [& {:keys [^Double diameter ^int bits ^int on] :or {diameter 1.0 bits 127 on 21}}]
    (let [randomer 
            (random-fn-with-seed 123456)
          buckets 
            (atom {:diameter diameter :bits bits :on on :randomer randomer :bins []})		
          encode! 
            (fn [^Double x] 
                (if-not (find-bucket! x buckets) (add-bucket! x buckets))
                (sort (:sdr (find-bucket! x buckets))))
          encode-to-bitstring! 
            (fn [^Double x] 
                (sdr->bitstring (encode! x) bits))]
        {:buckets buckets
         :encode encode!
         :encode-to-bitstring! encode-to-bitstring!}))
		