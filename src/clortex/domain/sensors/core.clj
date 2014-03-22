(ns clortex.domain.sensors.core
"
## [Pre-alpha] OPF-Style Sensors

Currently reads an OPF-style CSV file and converts it into Clojure data structures.

**TODO**: ?
"
  #_(:refer-clojure :exclude [second extend])
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clortex.domain.sensors.date :refer [parse-opf-date]]
            [clortex.domain.encoders.core :as enc]
            [clortex.domain.encoders.rdse :as rdse]))

(defn parse-opf-item
	"converts a CSV item (a string) into a Clojure value"
    [v t]
    (condp = t
	  "datetime" (parse-opf-date v)
	  "float" (double (read-string v))
	  v))

(defn safe-parse-opf-item
	"converts a CSV item (a string) into a Clojure value. catches and throws exceptions"
    [v t]
    (try (parse-opf-item v t)
	(catch Exception e (do (println (str "caught exception for value " v)) (throw e)))))

(defn parse-opf-row
	[line & {:keys [fields types flags]}]
	(vec (for [i (range (count line))]
	  (let [^String v (line i) ^String t (types i) ^String field (fields i) ^String flag (flags i)
	    parsed (parse-opf-item v t)
	    opf-meta {:raw v :type t :field field :flag flag}]
	    (with-meta
		  [parsed]
		  {:opf-meta opf-meta})))))

; type-map (apply hash-map (vec (interleave line types)))
(defn parse-opf-data
  "parse OPF data from CSV test rows"
  [raw-csv & {:keys [fields types flags]}]
  (mapv #(parse-opf-row % :fields fields :types types :flags flags) (drop 3 raw-csv)))


(defn make-encoder
	"converts a CSV item (a string) into a Clojure value"
    [field encoder-type]
    (condp = encoder-type
	  "datetime" (enc/date-encoder)
	  "float" (rdse/random-sdr-encoder-1)
	  (enc/hash-encoder)))

(defn make-encoders
  [fields types]
  (loop [inputs [fields types] result []]
    (if (empty? (first inputs))
      result
      (recur [(rest (first inputs)) (rest (second inputs))]
             (conj result (make-encoder (ffirst inputs) (first (second inputs))))))))

(defn load-opf-data [data & n]
  (let [raw-csv (if n (vec (take (first n) data))
                  (vec data))
        fields (raw-csv 0)
        types (raw-csv 1)
        flags (raw-csv 2)
        encoders (make-encoders fields types)
        opf-map {:fields fields :types types :flags flags :encoders encoders}
        parsed-data (parse-opf-data raw-csv :fields fields :types types :flags flags)
        ]
    (println "loaded" (count raw-csv) "lines")
       {:raw-csv raw-csv :fields fields :types types :flags flags
	    :parsed-data parsed-data
        :encoders encoders
	    }))

(defn load-opf-file [f & n]
  (let [fileio 	(with-open [in-file (io/reader f)]
	                  (vec (doall (csv/read-csv in-file))))
        n (if n n (count fileio))]
    (println "loaded" (count fileio) "lines")
       (load-opf-data fileio n)))

(comment
	(def hotgym (load-opf-file "resources/hotgym.csv"))
)

#_(with-open [out-file (io/writer "out-file.csv")]
  (csv/write-csv out-file
                 [["abc" "def"]
                  ["ghi" "jkl"]]))

