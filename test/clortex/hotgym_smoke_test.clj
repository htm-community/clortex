(ns clortex.hotgym-smoke-test
  (:use midje.sweet)
  (:require [clortex.core :as cla :refer :all]
            [datomic.api :as d]))

[[:chapter {:title "Introduction"}]]

[[:section {:title "Background"}]]

(def uri "datomic:free://localhost:4334/patches")
(def ^:dynamic *conn* (d/connect uri))

(defn lookup [?a _ a v]
  (let [d (d/db *conn*)]
    (map #(map :v (d/datoms d :eavt (:e %) ?a))
         (d/datoms d :avet a v))))

#_(bench
  (doall
    (lookup :universe/planet :by :universe/galaxy "Pegasus")))

(defn syn? [conn to from]
  (let [db (d/db conn)
        to-id (:db/id to)
        from-id (:db/id from)
        pre-key :synapse/pre-synaptic-neuron
        dendrite-key :dendrite/synapses
        dendrites (set (map :v (d/datoms db :eavt to-id :neuron/distal-dendrites)))
        ]
    (some dendrites (set (map :e (map #(first (d/datoms db :avet dendrite-key (:e %)))
         (d/datoms db :avet pre-key from-id)))))))

(defn syn? [conn to from]
  (let [db (d/db conn)
        to-id (:db/id to)
        from-id (:db/id from)
        pre-key :synapse/pre-synaptic-neuron
        dendrite-key :dendrite/synapses
        dendrites (set (map :v (d/datoms db :eavt to-id :neuron/distal-dendrites)))
        to-synapses (set (map :v (mapcat #(d/datoms db :eavt % :dendrite/synapses) dendrites)))
        synapses (set (map :e (d/datoms db :avet pre-key from-id)))]
    (some synapses to-synapses)))




