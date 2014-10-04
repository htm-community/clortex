(ns clortex.domain.sensors.datomic
"
## [Pre-alpha] Interface to Datomic

"
  (:require [datomic.api :as d]))


(defn input-sdr [db data-uuid timestep]
  (reduce (fn [s [v]] (conj s v)) #{} (d/q '[:find ?bit
         :in $ ?uuid ?timestep
         :where
         [?sdr-entry :sensor-data/uuid ?uuid]
         [?sdr-entry :sensor-data/bits ?bit]
         [?sdr-entry :sensor-data/timestep ?timestep]]
       db
       data-uuid
       timestep)))

(defn sdr->tx
  [data-uuid timestep sdr]
  (let [entry-id (d/tempid :db.part/user)
        entry-tx {:db/id entry-id
                  :sensor-data/uuid data-uuid
                  :sensor-data/timestep timestep}
        bit-txs (map (fn [bit] {:db/id entry-id
                                 :sensor-data/bits bit}) sdr)]
    (apply vector entry-tx bit-txs)))

(defn set-sdr [conn data-uuid timestep sdr]
  @(d/transact conn (sdr->tx data-uuid timestep sdr)))


