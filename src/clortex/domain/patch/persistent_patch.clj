(ns clortex.domain.patch.persistent-patch
  (:use [adi.utils :only [iid ?q]])
  (require [clortex.protocols :refer :all]
           [datomic.api :as d]
           [adi.core :as adi]))

(extend-type datomic.query.EntityMap PNeuronPatch
  (neurons [this] (:patch/neurons this))
  (neuron-with-index [this index]
   (filter #(= index (neuron-index %)) (neurons this)))
  (neuron-with-id [this id]
   (filter #(= id (neuron-id %)) (neurons this)))
  (columns [this] (:patch/columns this))
  (timestamp [this] (:patch/timestamp this))
  (set-input-sdr [this sdr] this)
  (connect-inputs [this] this)
  (feedforward-synapses [this] [])
  )

(defrecord DatomicPatch [patch-id patch conn]
  PNeuronPatch
  (neurons [this]
   (:patch/neurons patch))
  (neuron-with-index [this index]
   (filter #(= index (neuron-index %)) (neurons this)))
  (neuron-with-id [this id]
   (filter #(= id (neuron-id %)) (neurons this)))
  (columns [this] (:patch/columns patch))
  (timestamp [this] (:patch/timestamp patch))
  (set-input-sdr [this sdr] this)
  (connect-inputs [this] this)
  (feedforward-synapses [this] [])
)


(def empty-patch
  (->DatomicPatch nil nil nil))

(defn find-patch-id
  [ctx patch-uuid]
  (ffirst (d/q '[:find ?patch-id
                 :in $ ?p-uuid
                        :where [?patch-id :patch/uuid ?p-uuid]]
               (d/db (:conn ctx))
               patch-uuid)))

(defn load-patch [ctx ^DatomicPatch patch patch-id]
  (let [conn (:conn ctx)]
    (merge patch {:patch-id patch-id :conn conn :patch (d/entity (d/db conn) patch-id)})))

(defn load-patch-by-uuid [ctx patch-uuid]
  (let [conn (:conn ctx)
        patch-id (find-patch-id ctx patch-uuid)]
    (load-patch ctx empty-patch patch-id)))


(defn create-patch
  [ctx patch-uuid]
  (let [conn (:conn ctx)]
    @(d/transact conn [{:db/id (d/tempid :db.part/user)
                        :patch/uuid patch-uuid}])))


(defn create-adi-patch
  [ctx patch-uuid]
  (adi/insert! (:ds ctx) [{:patch {:uuid patch-uuid}}]))


      #_(let [uri "datomic:mem://adi-test"
            ds ds    (adi/datastore uri clortex-schema true true)
            _add  (adi/insert! ds [{:patch {:uuid patch-1}}])
            check (->> (adi/select ds {:patch/uuid patch-1})
                       first :patch :uuid)
            _tidy (d/delete-database uri)]
        check)


(defn find-patch-uuids
  [ctx]
  (let [conn (:conn ctx)]
    (d/q '[:find ?patch-uuid
           :where [_ :patch/uuid ?patch-uuid]]
         (d/db conn))))

(defn create-patch
  [ctx patch-uuid]
  (let [conn (:conn ctx)]
    @(d/transact conn [{:db/id (d/tempid :db.part/user)
                        :patch/uuid patch-uuid}])))


(defn find-neuron-id
  [ctx patch-id neuron-index]
  (ffirst (d/q '[:find ?neuron-id
                 :in $ ?patch ?neuron-index
                 :where [?patch :patch/neurons ?neuron-id]
                        [?neuron-id :neuron/index ?neuron-index]]
               (d/db (:conn ctx))
               patch-id
               neuron-index)))

(defn add-neuron
  [ctx patch-uuid]
  (let [conn (:conn ctx)
        patch-id (find-patch-id ctx patch-uuid)
        neurons (count
                 (d/q '[:find ?neuron
                        :in $ ?p-id
                        :where [?p-id :patch/neurons ?neuron]]
                      (d/db conn)
                      patch-id)
                     )
        neuron-id (d/tempid :db.part/user)]
    @(d/transact conn [{:db/id neuron-id
                        :neuron/index neurons
                        :neuron/feedforward-potential 0
                        :neuron/predictive-potential 0
                        :neuron/active? false}
                       {:db/id patch-id
                        :patch/neurons neuron-id}])))

(defn add-neurons-to
  [ctx patch-uuid n]
  (let [conn (:conn ctx)
        patch-id (find-patch-id ctx patch-uuid)
        neurons (count
                 (d/q '[:find ?neuron
                        :in $ ?p-id
                        :where [?p-id :patch/neurons ?neuron]]
                      (d/db conn)
                      patch-id)
                     )
        tx-tuples (for [i (range n)
                        :let [neuron-id (d/tempid :db.part/user)
                              neuron-index (+ i neurons)]]
                    [{:db/id neuron-id
                      :neuron/index neuron-index
                      :neuron/active? false}
                     {:db/id patch-id :patch/neurons neuron-id}])
        tx-data (reduce #(conj %1 (%2 0) (%2 1)) [] tx-tuples)]
    tx-data))

(defn add-inputs-to
  [ctx patch-uuid n]
  (let [conn (:conn ctx)
        ds (:ds ctx)
        patch-id (find-patch-id ctx patch-uuid)
        inputs (count
                 (d/q '[:find ?input
                        :in $ ?patch-id
                        :where
                        [?patch-id :patch/inputs ?dendrite]
                        [?dendrite :dendrite/synapses ?synapse]
                        [?synapse :synapse/pre-synaptic-neuron ?input]]
                      (d/db conn)
                      patch-id)
                     )
        dendrite-id (d/tempid :db.part/user)
        tx-dendrite (if (zero? inputs)
                      [{:db/id dendrite-id}
                       {:db/id patch-id :patch/inputs dendrite-id}]
                      [])
        tx-tuples (for [i (range n)
                        :let [input-id (d/tempid :db.part/user)
                              synapse-id (d/tempid :db.part/user)
                              input-index (+ i inputs)]]
                    [{:db/id input-id
                      :neuron/index input-index
                      :neuron/active? false}
                     {:db/id synapse-id
                      :synapse/pre-synaptic-neuron input-id
                      :synapse/permanence 1
                      :synapse/permanence-threshold 0}
                     {:db/id dendrite-id :dendrite/synapses synapse-id}])
        tx-data (reduce #(conj %1 (%2 0) (%2 1)) tx-dendrite tx-tuples)]
    (println tx-data)
    tx-data))

(defn add-neurons-to!
  [ctx patch-uuid n]
  @(d/transact (:conn ctx) (add-neurons-to ctx patch-uuid n)))

(defn add-inputs-to!
  [ctx patch-uuid n]
  @(d/transact (:conn (:ds ctx)) (add-inputs-to ctx patch-uuid n)))

(defn find-dendrites
  [ctx neuron-id]
  (let [conn (:conn ctx)]
    (d/q '[:find ?dendrite
           :in $ ?neuron
           :where [?neuron :neuron/distal-dendrites ?dendrite]]
         (d/db conn)
         neuron-id)))

(defn add-dendrite!
  [ctx neuron]
  (let [conn (:conn ctx)
        dendrite-id (d/tempid :db.part/user)]
    @(d/transact conn [{:db/id neuron :neuron/distal-dendrites dendrite-id}
                       {:db/id dendrite-id :dendrite/capacity 32}])
    ;(println "Added dendrite" dendrite-id "to neuron" neuron)
    (find-dendrites ctx neuron)))


(defn synapse-between
  ([ctx patch-uuid from to]
  (let [conn (:conn ctx)]
    ;(println "checking synapse from neuron " from-id "to" to-id)
    (d/q '[:find ?synapse
           :in $ ?to-index ?from-index ?patch-uuid
           :where
           [?patch :patch/uuid ?patch-uuid]
           [?patch :patch/neurons ?to]
           [?to :neuron/index ?to-index]
           [?patch :patch/neurons ?from]
           [?from :neuron/index ?from-index]
           [?to :neuron/distal-dendrites ?dendrite]
           [?dendrite :dendrite/synapses ?synapse]
           [?synapse :synapse/pre-synaptic-neuron ?from]]
         (d/db conn)
         to from patch-uuid)))
  ([db from to]
   ;(println "checking synapse from neuron " (:db/id to) "from" (:db/id from))
    (d/q '[:find ?synapse
           :in $ ?to ?from
           :where
             [?to :neuron/distal-dendrites ?dendrite]
             [?dendrite :dendrite/synapses ?synapse]
             [?synapse :synapse/pre-synaptic-neuron ?from]]
         db
         (:db/id to) (:db/id from))))

#_(bench (d/q '[:find ?synapse :in $ ?to ?from :where [?to :neuron/distal-dendrites ?dendrite][?dendrite :dendrite/synapses ?synapse][?synapse :synapse/pre-synaptic-neuron ?from]] (d/db *conn*) (:db/id to) (:db/id from)))
#_(bench (d/q '[:find ?synapse :in $ ?to ?from :where [?to :neuron/distal-dendrites ?dendrite][?synapse :synapse/pre-synaptic-neuron ?from][?dendrite :dendrite/synapses ?synapse]] (d/db *conn*) (:db/id to) (:db/id from)))

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

(defn enervated-by [db from]
  (let [from-id (:db/id from)
        pre-key :synapse/pre-synaptic-neuron
        synapses (map :e (d/datoms db :avet pre-key from-id))
        dendrite-key :dendrite/synapses
        dendrites (map :e (mapcat #(d/datoms db :avet dendrite-key %) synapses))
        to-neurons (map :e (mapcat #(d/datoms db :avet :neuron/distal-dendrites %) dendrites))
        ]
    [from to-neurons synapses]))


(defn connect-distal
  [ctx patch-uuid from to async?]
  (when (zero? (count (synapse-between ctx patch-uuid from to)))
   (let [conn (:conn ctx)
        randomer (:randomer ctx)
        patch-id (find-patch-id ctx patch-uuid)
        from-id (find-neuron-id ctx patch-id from)
        to-id (find-neuron-id ctx patch-id to)
        synapse-id (d/tempid :db.part/user)
        permanence-threshold 0.2
        permanent? (> (randomer 3) 0)
        permanence (* permanence-threshold (if permanent? 1.1 0.9))
        synapse-tx {:db/id synapse-id
                    :synapse/pre-synaptic-neuron from-id
                    :synapse/permanence permanence
                    :synapse/permanence-threshold permanence-threshold}
        dendrites (find-dendrites ctx to-id)
        dendrites (if (empty? dendrites)
                    (add-dendrite! ctx to-id)
                    dendrites)
        dendrite (ffirst dendrites)]
    ;(println "Connecting " from-id "->" to-id "Adding synapse" synapse-id "to dendrite" dendrite)
    (if async?
      (d/transact-async conn [{:db/id dendrite :dendrite/synapses synapse-id}
                       synapse-tx])
      @(d/transact conn [{:db/id dendrite :dendrite/synapses synapse-id}
                       synapse-tx])))))

(defn new-dendrite-tx
  [to]
  (let [dendrite-id (d/tempid :db.part/user)]
    {:txs [{:db/id dendrite-id}
           {:db/id (:db/id to)
            :neuron/distal-dendrites dendrite-id}]
     :id dendrite-id}))

(defn connect-distal-tx
  [ctx from to]
  (let [conn (:conn ctx)
        db (d/db conn)]
    (when (nil? (syn? conn to from))
      (let [randomer (:randomer ctx)
            synapse-id (d/tempid :db.part/user)
            to-id (:db/id to)
            permanence-threshold 0.2
            permanent? (> (randomer 3) 0)
            permanence (* permanence-threshold (if permanent? 1.1 0.9))
            synapse-tx {:db/id synapse-id
                        :synapse/pre-synaptic-neuron (:db/id from)
                        :synapse/permanence permanence
                        :synapse/permanence-threshold permanence-threshold}
            dendrites (map :v (d/datoms db :eavt to-id :neuron/distal-dendrites))
            new-dendrite-data (if (empty? dendrites)
                                (new-dendrite-tx to))
            dendrite-tx (if (empty? dendrites)
                          (:txs new-dendrite-data)
                          [])
            dendrite (if (empty? dendrites)
                       (:id new-dendrite-data)
                       (first dendrites))]
        ;(println "Connecting " from-id "->" to-id "Adding synapse" synapse-id "to dendrite" dendrite)
        (concat [{:db/id dendrite :dendrite/synapses synapse-id}
                     synapse-tx]
               dendrite-tx)))))


#_(defn connect-distal-txs
  [ctx froms tos chance]
  ;(when (zero? (count (synapse-between ctx from to)))
  (let [conn (:conn ctx)
        randomer (:randomer ctx)
        synapses (d/q )
        synapse-id (d/tempid :db.part/user)
        permanence-threshold 0.2
        permanent? (> (randomer 3) 0)
        permanence (* permanence-threshold (if permanent? 1.1 0.9))
        synapse-tx {:db/id synapse-id
                    :synapse/pre-synaptic-neuron from-id
                    :synapse/permanence permanence
                    :synapse/permanence-threshold permanence-threshold}
        dendrites (find-dendrites ctx to-id)
        dendrites (if (empty? dendrites)
                    (add-dendrite! ctx to-id)
                    dendrites)
        dendrite (ffirst dendrites)]
    ;(println "Connecting " from-id "->" to-id "Adding synapse" synapse-id "to dendrite" dendrite)
    (if async?
      (d/transact-async conn [{:db/id dendrite :dendrite/synapses synapse-id}
                       synapse-tx])
      @(d/transact conn [{:db/id dendrite :dendrite/synapses synapse-id}
                       synapse-tx]))))

(defn find-neurons
  [ctx patch-uuid]
  (let [conn (:conn ctx)
        patch-id (find-patch-id ctx patch-uuid)]
    (d/q '[:find ?neuron-index
           :in $ ?patch-id
           :where [?patch-id :patch/neurons ?neuron-id]
           [?neuron-id :neuron/index ?neuron-index]]
         (d/db conn)
         patch-id)))

(defn input-sdr
  [ctx patch-uuid]
  (let [conn (:conn ctx)]
    (d/q '[:find ?index ?active
           :in $ ?patch-uuid
           :where
           [?patch :patch/uuid ?patch-uuid]
           [?patch :patch/inputs ?dendrite]
           [?dendrite :dendrite/synapses ?synapse]
           [?synapse :synapse/pre-synaptic-neuron ?input]
           [?input :neuron/active? ?active]
           [?input :neuron/index ?index]]
         (d/db conn)
         patch-uuid)))

(defn find-neuron-ids
  [ctx patch-uuid]
  (let [conn (:conn ctx)
        patch-id (find-patch-id ctx patch-uuid)]
    (d/q '[:find ?neuron-id
           :in $ ?patch-id
           :where [?patch-id :patch/neurons ?neuron-id]]
         (d/db conn)
         patch-id)))


