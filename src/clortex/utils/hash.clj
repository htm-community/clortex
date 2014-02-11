(ns clortex.utils.hash
  (:import (java.security MessageDigest)))

(defn mod-2
  [num div]
  (let [m (rem num div)]
    (if (or (zero? m) (= (pos? num) (pos? div)))
      m
      (if (pos? div) (+ m div) m))))

(defn get-bytes [s] (byte-array (map (comp byte int) s)))

(defn sha1 [obj]
  (let [bytes (get-bytes (with-out-str (pr obj)))] 
    (apply vector (.digest (MessageDigest/getInstance "SHA1") bytes))))