(ns clortex.domain.neuron.persistent-neuron
  (require [clortex.protocols :refer :all]))

(extend-type datomic.query.EntityMap PNeuron
  (neuron-index [this] (:neuron/index this))
  (neuron-id [this] (:neuron/uuid this))
  (distal-dendrites [this] (:neuron/distal-dendrites this))
  (proximal-dendrite [this] (:neuron/proximal-dendrite this))
  )


(defn connect-feedforward
  "adds a feedforward synapse on neuron 'to' from neuron 'from'"
  [to from]
  (let [neuron to
         from-index (.neuron-index from)
         perm (/ (rand-int 256) 256.0)]
     (assoc-in neuron
               [:neuron/proximal-dendrite 0]
               {:synapse/pre-synaptic-neuron from-index,
                :synapse/permanence perm})))
