(ns clortex.domain.patch.pure-patch
  (:require [clortex.protocols :refer :all]
            [clortex.domain.neuron.pure-neuron :as n]))

(defrecord PurePatch [uuid neurons columns inputs outputs synapses timestamp]
  PNeuronPatch
  (neurons [this] (:neurons this))
  (neuron-with-index [this index]
   (filter #(= index (.neuron-index %)) (neurons this)))
  (neuron-with-id [this id]
   (filter #(= id (.neuron-id %)) (neurons this)))
  (set-neurons [this neurons] (assoc this :neurons neurons))
  (columns [this] (:columns this))
  (timestamp [this] (:timestamp this))
  (set-input-sdr [this sdr] this)
  (connect-inputs [this] this)
  (feedforward-synapses [this] [])
  )

(comment
  (neurons [p] "returns a collection of the patch's neurons")
  (neuron-with-index [p index] "returns a neuron with given index (or nil)")
  (neuron-with-id [p id] "returns a neuron with given uuid (or nil)")
  (set-neurons [p neurons] "returns patch with neuron added")
  (columns [p] "collection of mini-columns")
  (timestamp [p])
  (set-input-sdr [p sdr] "returns a patch with inputs matched to sdr")
  (connect-inputs [p] "returns a patch with inputs connected to proximal dendrites")
  (feedforward-synapses [p] "returns a collection of neurons affected by inputs"))

(def empty-patch
  (->PurePatch nil [] [] [] [] {} -1))

(defn patch
  "returns a patch (empty or merged with inps)"
  [& inps]
  (merge empty-patch (if inps (apply hash-map inps) {})))

