(ns clortex.protocols)

(defprotocol PNeuronPatch
  "Protocol for basic patch (Layer/Region) operations"
  (neurons [p] "returns a collection of the patch's neurons")
  (neuron-with-index [p index] "returns a neuron with given index (or nil)")
  (neuron-with-id [p id] "returns a neuron with given uuid (or nil)")
  (set-neurons [p neurons] "returns patch with neuron added")
  (columns [p] "collection of mini-columns")
  (timestamp [p])
  (set-input-sdr [p sdr] "returns a patch with inputs matched to sdr")
  (connect-inputs [p] "returns a patch with inputs connected to proximal dendrites")
  (feedforward-synapses [p] "returns a collection of neurons affected by inputs")
  )

(defprotocol PNeuron
  "Protocol for Cortical Learning Algorithm Neurons"
  (neuron-index [n] "index of this neuron within its patch")
  (neuron-id [n] "uuid of this neuron")
  (distal-dendrites [n] "collection of distal dendrites for this neuron")
  (proximal-dendrite [n]))

(defprotocol PDendriteSegment
  "Protocol for CLA dendrites"
  (synapses [d])
  (add-synapse [d s])
  (capacity [d])
  (full? [d])
  )

(defprotocol PPersistable
  "Protocol for persistable object")


(defprotocol CLAEncoder
  "Encodes values into bit representations"
  (bits [this] "returns width in bits of this encoder")
  (on-bits [this] "returns number of on-bits of this encoder")
  (field-name [this] "returns the field (using . to indicate hierarchy)")
  (encoders [this] "returns the bit encoder functions")
  (encode-all [this value] "returns a verbose data structure for an encoding of value")
  (encode [this value] "returns a set of on-bits encoding value"))

(extend-type Object
  CLAEncoder
  (bits [this] (:bits this))
  (on-bits [this] (:on-bits this))
  (field-name [this] (:field-name this))
  (encoders [this] (:encoders this))
  (encode-all [this value] (mapv #(vector % ((.encoders this %) value)) (range (.bits this))))
  (encode [this value] (set (vec (map first (filter second (.encode-all this value)))))))

#_(extend-type IPersistentVector
  CLAEncoder
  (bits [this] (map (comp + bits) this))
  (on-bits [this] (map (comp + bits) this))
  (field-name [this] (:field-name this))
  (encoders [this] (:encoders this))
  (encode-all [this value] (mapv #(vector % ((.encoders this %) value)) (range (.bits this))))
  (encode [this value] (set (vec (map first (filter second (.encode-all this value)))))))
