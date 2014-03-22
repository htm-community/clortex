(ns clortex.domain.neuron
	(:require [clortex.utils.uuid :as uuid]
           [clortex.utils.math :refer :all]
           [clortex.domain.neuron.pure-neuron :as pure-n]
           [clortex.domain.patch.persistent-patch :as db-patch]
           [datomic.api :as d]))

(defn free-db []
  (let [uri "datomic:free://macbook.local:4334/patches"]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)
          schema (load-file "resources/datomic/schema.edn")]
      (d/transact conn schema)
      conn)))

(defn dendrite
  "creates a new dendrite"
  []
  {})



(defn connect-axon
  "adds an axon connection on neuron 'from' to neuron 'to'"
  [from to]
  (let [neuron to
         to-index (:neuron/index to)]
     (assoc-in neuron
               [:neuron/axon]
               {:axon/to to-index,
                :axon/signalled 0})))

(defn get-neuron
  "retrieves the neuron at position pos in patch"
  [patch pos]
  (patch pos))



(comment
  (def p (neuron-patch 20))
  (count p)
  (def from (get-neuron p 3))
  (def to (get-neuron p 5))
  (connect-feedforward to from)
  (connect-axon from to)
(use 'clortex.neuron)

(def p (neuron-patch 1024))
(def c (/ (count p) 2))

(defn connections [patch n] (dotimes [i n] (let [x (get-neuron patch (rand-int c))
                        y (get-neuron patch (+ c (rand-int c))) ]
                    (connect-feedforward x y)
                    (connect-axon y x)))
      (println "done"))
)
