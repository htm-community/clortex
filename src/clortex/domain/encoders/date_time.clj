(ns clortex.domain.encoders.date-time
"
## [Pre-alpha] Date-Time Encoders
"
  (:require [clortex.protocols :refer :all]
            [clortex.domain.encoders.scalar :as s]
            [clortex.domain.encoders.compound :as c]
            [clj-time.core :as tc]))


(defn temporal-encoder
  [fieldname converter encoder]
  (fn [t]
    (encoder (converter t))))


#_(defn date-encoder
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

(defn time-of-day
  [t]
  (let [y (tc/year t)
        m (tc/month t)
        d (tc/day t)
        start-of-day (tc/date-time y m d)]
    (tc/in-seconds (tc/interval start-of-day t))))

(defn time-of-day-encoder
  [bits on]
  (let [encoder (s/scalar-encoder :bits bits :on on :minimum 0 :maximum 86400)
        encode (fn [t] ((:encode encoder) (time-of-day t)))]
    {:encode encode
     :bits bits
     :on on}))

(defn weekday-encoder
  [bits on]
  (let [encoder (s/scalar-encoder :bits bits :on on :minimum 1 :maximum 7)
        encode (fn [t] ((:encode encoder) (tc/day-of-week t)))]
    {:encode encode
     :bits bits
     :on on}))


(defn opf-date-encoder
  "constructs functions to encode date and time values. Takes keyword-mapped specs for
  various encodings"
  ;[& {:keys [time-of-day day-of-week time-of-year season] :or {bits 127 on 21}}]
  [& {:keys [time-of-day day-of-week time-of-year season] :or {time-of-day false day-of-week false time-of-year false season false}}]
  (let [weekday-enc (weekday-encoder 70 21)
        time-of-day-enc (time-of-day-encoder 1024 21)
        encs [weekday-enc time-of-day-enc]
        encode (fn [t] (c/combined-enc t encs))]
    {:encode encode
     :bits (reduce + (map :bits encs))
     :on (reduce + (map :on encs))}))

