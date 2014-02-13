(ns clortex.domain.sensors.core
  #_(:refer-clojure :exclude [second extend])
  (:require [clojure.data.csv :as csv]
	[clojure.java.io :as io] 
	[clortex.domain.sensors.date :refer [parse-opf-date]]
    #_[clortex.domain.sensor.encoders.core :as enc :refer :all]))

(defn safe-parse-opf-item 
    [v t] 
    (try (condp = t
	  "datetime" (parse-opf-date v)
	  "float" (double (read-string v)) 
	  v)
	(catch Exception e (do (println (str "caught exception for value " v)) (throw e)))))

(defn parse-opf-item 
	    [v t] 
	    (condp = t
		  "datetime" (parse-opf-date v)
		  "float" (double (read-string v)) 
		  v))
	
(defn parse-opf-row
	[line & {:keys [fields types flags]}]
	(for [i (range (count line))]
	  (let [^String v (line i) ^String t (types i) ^String field (fields i) ^String flag (flags i)
	    parsed (parse-opf-item v t)
	    opf-meta {:raw v :type t :field field :flag flag}]
	    (with-meta 
		  [parsed]
		  {:opf-meta opf-meta}))))
		
; type-map (apply hash-map (vec (interleave line types)))
(defn parse-opf-data
  "parse OPF data from CSV test rows"
  [raw-csv & {:keys [fields types flags]}]
  (map #(parse-opf-row % :fields fields :types types :flags flags) (drop 3 raw-csv)))

(defn load-opf-file [f & n] 
  (let [fileio 	(with-open [in-file (io/reader f)]
	                  (vec (doall (csv/read-csv in-file))))
        raw-csv (if n (vec (take (first n) fileio))
                  (vec fileio))
        fields (raw-csv 0)
        types (raw-csv 1)
        flags (raw-csv 2)
        opf-map {:fields fields :types types :flags flags}
        parsed-data (parse-opf-data raw-csv :fields fields :types types :flags flags)
        ]
       {:raw-csv raw-csv :fields fields :types types :flags flags
	    :parsed-data parsed-data
	    }))
	
(defn load-opf-data [data & n] 
  (let [raw-csv (if n (vec (take (first n) data))
                  (vec data))
        fields (raw-csv 0)
        types (raw-csv 1)
        flags (raw-csv 2)
        opf-map {:fields fields :types types :flags flags}
        parsed-data (parse-opf-data raw-csv :fields fields :types types :flags flags)
        ]
       {:raw-csv raw-csv :fields fields :types types :flags flags
	    :parsed-data parsed-data
	    }))

(comment 
	(def hotgym (load-opf-file "resources/hotgym.csv"))
)

#_(with-open [out-file (io/writer "out-file.csv")]
  (csv/write-csv out-file
                 [["abc" "def"]
                  ["ghi" "jkl"]]))

