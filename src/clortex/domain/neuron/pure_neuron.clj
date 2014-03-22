(ns clortex.domain.neuron.pure-neuron
  (:require [clortex.protocols :as cp]))

(defrecord PureNeuron
  [uuid index
   distal-dendrites proximal-dendrite
   active? predictive-potential activation-potential]
  cp/PNeuron
  (neuron-index [this] (:index this))
  (neuron-id [this] (:uuid this))
  (distal-dendrites [this] (:distal-dendrites this))
  (proximal-dendrite [this] (:proximal-dendrite this))
  )


(def empty-neuron
  (->PureNeuron nil -1 [] [] false 0 0))

(defn neuron
  "returns a neuron (empty or merged with inps)"
  [& inps]
  (merge empty-neuron (if inps (apply hash-map inps) {})))
