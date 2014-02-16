(ns clortex.domain.encoders.rdse
  (:require [clortex.utils.math :refer :all]
	[clojure.set :refer [difference]]))

(defn bottom-sorter [x y]
	(println (str x " vs " y))
	(let [c (compare (x :bottom) (y :bottom))]
		(if (not= c 0)
			c
			(compare x y))))

(defn ordered-bins [bins] (sort-by bottom-sorter bins))

(defn find-bucket! 
	[value buckets] 
	(when-let [bucket (first (filter #(<= (:bottom %) value (:top %)) (:bins @buckets)))]
		(swap! buckets update-in [:bins (:index bucket) :read] inc)
		bucket))

(defn new-bucket 
	[value radius index]
	{:bottom (- value radius) :top (+ value radius) :index index :counter 1 :read 0})

(defn abs-diff [x y] (. Math abs (- x y)))

(defn min-distance 
	[acc a-bucket] 
	;(println (str "acc\n" acc "\na-bucket\n" a-bucket))
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

(defn new-sdr 
	[bucket buckets]
	(let [bins (:bins buckets)
	      on (:on buckets)
	      bits (:bits buckets)
	      randomer (:randomer buckets)]
	  (if (empty? bins) 
	    (vec (range on))
	    (let [nearest-find (reduce min-distance (search-starter bucket) bins)
;	          sorted-bins (ordered-bins bins)
;dbg (println (str "nearest-find\n" nearest-find))
;dbg (println (str "sorted\n" (map :bottom sorted-bins)))
	          nearest (bins (:index nearest-find))
			  nearest-sdr (:sdr nearest)
			  change-bit (if (> (:bottom bucket) (:bottom nearest)) 
			                 (first nearest-sdr) 
			                 (last nearest-sdr))
			   free-bits (difference (set (range bits)) (set nearest-sdr))
			   new-bit-pos (randomer (count free-bits))
;dbg (println (str "new-bit-pos\t" new-bit-pos "\nfree-bits\t" free-bits))
			
			   new-bit ((vec free-bits) new-bit-pos)
;dbg (println (str "nearest\t" nearest "\nbucket\t" bucket "\nnearest-sdr\t" nearest-sdr))
			   new-sdr (vec (conj (disj (set nearest-sdr) change-bit) new-bit))
              ]
		  new-sdr))))

(defn add-to-buckets! 
	[buckets bucket]
	(let [sdr (new-sdr bucket @buckets)
          sdr-bucket (conj bucket {:sdr sdr})]
;(println (str "adding bucket\t" sdr-bucket))
	(swap! buckets update-in [:bins] conj sdr-bucket)))	

(defn add-bucket! 
	[value buckets]
	(let [diameter (:diameter @buckets)
	      radius (/ diameter 2.0)
	      mn #(bottom-of-buckets (:bins @buckets))
		  mx #(top-of-buckets (:bins @buckets))]
;(println (str "buckets:\n" @buckets))
	  (if (empty? (:bins @buckets))
	    (let [bucket (new-bucket value radius (n-bins @buckets))]
	;(println (str "adding bucket:\n" bucket))
	      (add-to-buckets! buckets bucket))
	    (do (while (> value (mx))
		        (add-to-buckets! buckets (new-bucket (+ (mx) radius) radius (n-bins @buckets))))
		     (while (< value (mn)) 
		        (add-to-buckets! buckets (new-bucket (- (mn) radius) radius (n-bins @buckets))))
		))))

(defn random-sdr-encoder-1
	[& {:keys [diameter bits on] :or {diameter 1.0 bits 127 on 21}}]
	(let [randomer (random-fn-with-seed 123456)
		  buckets (atom {:diameter diameter :bits bits :on on :randomer randomer :bins []})		
		  encode-to-bitstring! 
		    (fn [x] 
			  (if-not (find-bucket! x buckets) (add-bucket! x buckets))
			  (let [sdr (set (:sdr (find-bucket! x buckets)))]
			     (apply str (vec (map #(if (contains? sdr %) 1 0) (range bits))))))]
		{:buckets buckets
		 :encode-to-bitstring! encode-to-bitstring!}))
		