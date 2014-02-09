(ns clortex.neuron)

(defn new-dendrite
  "creates a new dendrite"
  []
  {})

(defn new-neuron
  "returns an empty neuron"
  ;[id & {:patch patch :or nil}]
  [id]
  {:id id,
   :proximal {0 (new-dendrite)},
   :distal [(new-dendrite)],
   :axon #{},
   :activation 0,
   :prediction 0,
   :patch nil})

(defn patch
  "returns a patch of n empty neurons"
  [n]
  (reduce
     (fn [m v] (assoc m v (new-neuron v)))
     {}
     (range n)))

(defn connect-feedforward
  "adds a feedforward synapse on neuron 'to' from neuron 'from'"
  [to from]
  (let [neuron to
         from-index (:id from)
         perm (/ (rand-int 256) 256.0)]
     (assoc-in neuron
               [:proximal 0]
               {:from from-index,
                :permanence perm})))

(defn connect-axon
  "adds an axon connection on neuron 'from' to neuron 'to'"
  [from to]
  (let [neuron to
         to-index (:id to)]
     (assoc-in neuron
               [:axon]
               {:to to-index,
                :signalled 0})))

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
