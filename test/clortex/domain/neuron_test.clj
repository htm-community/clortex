(ns clortex.domain.neuron-test
  (:use midje.sweet)
  (:require [clortex.domain.neuron :as n]
            [datomic.api :as d]
            [clortex.utils.math :refer :all]
            [clortex.utils.datomic :refer :all]
            [clortex.domain.patch.pure-patch :as purep]
            [clortex.domain.neuron.pure-neuron :as pure-neuron]
            [clortex.domain.patch.persistent-patch :as dbp]
            [adi.core :as adi]))


(fact "create a pure patch"
(def n 1024)
(def p (.set-neurons (purep/patch) (repeat n (pure-neuron/neuron))))
(count (.neurons p)) => n
)

(defn create-in-memory-db []
  (let [uri "datomic:mem://patch-test-db"]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)
          schema (load-file "resources/datomic/schema.edn")]
      (d/transact conn schema)
      conn)))


(defn create-free-db []
  (let [uri "datomic:free://localhost:4334/patches"]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)
          schema (load-file "resources/datomic/schema.edn")]
      (d/transact conn schema)
      conn)))

(defn create-free-db
  "ADI version"
  []
  (let [uri "datomic:free://localhost:4334/patches"]
    (d/delete-database uri)
    (d/create-database uri)
    (adi/datastore uri clortex-schema true true)))

(defn create-adi-in-memory-db []
  (let [uri "datomic:mem://patch-adi-db"]
    (adi/datastore uri clortex-schema true true)))

(def patch-1 (d/squuid))
(def patch-2 (d/squuid))
(def patch-3 (d/squuid))

(def patch-4 (d/squuid))

(fact "create an adi-based db, add a patch"
      (let [uri "datomic:mem://adi-test"
            ;_ (println "Loading ADI Schema")
            ds (adi/datastore uri clortex-schema true true)
            _add  (adi/insert! ds [{:patch {:uuid patch-1}}])
            check (->> (adi/select ds {:patch/uuid patch-1})
                       first :patch :uuid)
            _tidy (d/delete-database uri)]
        check) => patch-1
)


(fact "After creating a patch, we can find the patch"
      (let [ctx {:conn (create-in-memory-db)}
            _ (dbp/create-patch ctx patch-1)
            patch (dbp/load-patch-by-uuid ctx patch-1)]
        (:patch/uuid (:patch patch))) => patch-1
)

(fact "After creating several patches, we can find them all"
      (let [ctx {:conn (create-in-memory-db)}]
        (dbp/create-patch ctx patch-1)
        (dbp/create-patch ctx patch-2)
        (dbp/create-patch ctx patch-3)
        (dbp/find-patch-uuids ctx)) => #{[patch-1] [patch-2] [patch-3]}
)

(fact "Adding a neuron to a patch, we can find the neuron"
      (let [ctx {:conn (create-in-memory-db)}]
        (dbp/create-patch ctx patch-1)
        (dbp/add-neuron ctx patch-1)
        (dbp/find-neurons ctx patch-1)) => #{[0]}
      )

(fact "Adding several neurons to patches, we can find the neurons"
      (let [ctx {:conn (create-in-memory-db)}]
        (dbp/create-patch ctx patch-1)
        (dbp/add-neuron ctx patch-1)
        (dbp/create-patch ctx patch-2)
        (dbp/add-neuron ctx patch-2)
        (dbp/add-neuron ctx patch-1)
        (dbp/add-neuron ctx patch-1)
        (dbp/find-neurons ctx patch-1)) => #{[0] [1] [2]}
      )

(fact "Adding many neurons to a patch, we can find the neurons"
      (def n 65536)
      (def n 1024)
      (let [ctx {:conn (create-in-memory-db)}]
        (dbp/create-patch ctx patch-1)
        (time (dbp/add-neurons-to! ctx patch-1 n))
        (count (dbp/find-neurons ctx patch-1))) => n
      )

(fact "Connecting one neuron to another, we can find the synapse"
      (let [ctx {:conn (create-in-memory-db) :randomer (random-fn-with-seed 123456)}]
        (dbp/create-patch ctx patch-1)
        (dbp/add-neuron ctx patch-1)
        (dbp/add-neuron ctx patch-1)
        (dbp/connect-distal ctx patch-1 0 1 false)
        (count (dbp/synapse-between ctx patch-1 0 1))) => 1
      )

(fact "Connecting many neurons to one another, we can find the synapses"
      (let [ds (create-free-db)
            ctx {:ds ds :conn (:conn ds) :randomer (random-fn-with-seed 123456)}
            n 32768
            ;n 8192
            randomer (random-fn-with-seed 123456)]
        (dbp/create-patch ctx patch-1)
        (dbp/add-neurons-to! ctx patch-1 (* n 2))
        (dbp/connect-distal ctx patch-1 0 1 false)
        (time (doseq [i (range 100)]
          (dbp/connect-distal ctx patch-1 (randomer n) (+ n (randomer n)) false)))
        (count (dbp/synapse-between ctx patch-1 0 1))) => 1
        ;(dbp/add-inputs-to! ctx patch-1 )
      )

(fact "Adding inputs to a patch, we can write and read the SDR"
      (let [ds (create-adi-in-memory-db)
            ctx {:ds ds :conn (:conn ds) :randomer (random-fn-with-seed 123456)}]
        (dbp/create-adi-patch ctx patch-1)
        (dbp/add-inputs-to! ctx patch-1 12)
        (def sdr #{1 3 5 7 11})
        (dbp/set-input-bits ctx patch-1 sdr)
        (dbp/input-sdr ctx patch-1) => sdr
        (def sdr2 #{1 3 6 9 10})
        (dbp/set-input-bits ctx patch-1 sdr2)
        (dbp/input-sdr ctx patch-1) => sdr2
      ))


#_(comment
    "
user=> (bench (syn? conn 0 1))
WARNING: JVM argument TieredStopAtLevel=1 is active, and may lead to unexpected results as JIT C2 compiler may not be active. See http://www.slideshare.net/CharlesNutter/javaone-2012-jvm-jit-for-dummies.
WARNING: Final GC required 5.415848803839964 % of runtime
Evaluation count : 78600 in 60 samples of 1310 calls.
             Execution time mean : 796.580338 µs
    Execution time std-deviation : 106.084681 µs
   Execution time lower quantile : 723.190637 µs ( 2.5%)
   Execution time upper quantile : 1.106124 ms (97.5%)
                   Overhead used : 9.878120 ns

Found 8 outliers in 60 samples (13.3333 %)
	low-severe	 2 (3.3333 %)
	low-mild	 6 (10.0000 %)
 Variance from outliers : 80.6874 % Variance is severely inflated by outliers

user=> (bench (syn-using-query? conn 0 1))
WARNING: JVM argument TieredStopAtLevel=1 is active, and may lead to unexpected results as JIT C2 compiler may not be active. See http://www.slideshare.net/CharlesNutter/javaone-2012-jvm-jit-for-dummies.
Evaluation count : 12420 in 60 samples of 207 calls.
             Execution time mean : 5.033397 ms
    Execution time std-deviation : 338.335985 µs
   Execution time lower quantile : 4.684154 ms ( 2.5%)
   Execution time upper quantile : 5.871142 ms (97.5%)
                   Overhead used : 9.878120 ns

Found 4 outliers in 60 samples (6.6667 %)
	low-severe	 4 (6.6667 %)
 Variance from outliers : 50.1281 % Variance is severely inflated by outliers

user=> (bench (syn-using-query? conn 0 1))
WARNING: JVM argument TieredStopAtLevel=1 is active, and may lead to unexpected results as JIT C2 compiler may not be active. See http://www.slideshare.net/CharlesNutter/javaone-2012-jvm-jit-for-dummies.
Evaluation count : 2280 in 60 samples of 38 calls.
             Execution time mean : 27.199456 ms
    Execution time std-deviation : 596.419497 µs
   Execution time lower quantile : 26.584016 ms ( 2.5%)
   Execution time upper quantile : 28.893191 ms (97.5%)
                   Overhead used : 9.878120 ns

Found 6 outliers in 60 samples (10.0000 %)
	low-severe	 4 (6.6667 %)
	low-mild	 2 (3.3333 %)
 Variance from outliers : 9.4584 % Variance is slightly inflated by outliers

    "
    )
