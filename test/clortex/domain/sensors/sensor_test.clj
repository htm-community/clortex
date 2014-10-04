(ns clortex.domain.sensors.sensor-test
  (:use midje.sweet)
  (:use clortex.domain.sensors.core)
  (:require [clortex.domain.encoders.compound :as c]
            [clojure.set :refer [difference intersection]]
            [datomic.api :as d]
            [clortex.domain.sensors.datomic :as s->db]))

[[:chapter {:title "Sensors"}]]

"Sensors gather information from the world and deliver it in encoded form to the CLA.

"

[[:section {:title "Numenta OPF Sensor (CSV data)"}]]

"The first sensor in `clortex` reads CSV data which is compatible with Numenta's OPF
(Online Prediction Framework) software. "

[[:code "gym,address,timestamp,consumption
string,string,datetime,float
S,,T,
Balgowlah Platinum,Shop 67 197-215 Condamine Street Balgowlah 2093,2010-07-02 00:00:00.0,5.3
Balgowlah Platinum,Shop 67 197-215 Condamine Street Balgowlah 2093,2010-07-02 00:15:00.0,5.5
Balgowlah Platinum,Shop 67 197-215 Condamine Street Balgowlah 2093,2010-07-02 00:30:00.0,5.1
Balgowlah Platinum,Shop 67 197-215 Condamine Street Balgowlah 2093,2010-07-02 00:45:00.0,5.3
" {:title "a very simple scalar encoder" :tag "simple-scalar-encoder"}]]

"The first line lists the field names for the data in the file. These field names are referenced elsewhere
when specifying the field(s) which need to be predicted, or the encoders to use for that field. The second
line describes the type fo each field (in Python terms). The third line is OPF-specific. `S` (referring to
the `gym` field) indicates that this field, when it changes, indicates a new **sequence** of data records.
The `T` (for the `timestamp` field) indicates that this field is to be treated as time-series data. These two
concepts are important in powering the CLA's sequence learning.
"
[[:file {:src "test/clortex/domain/sensors/date_test.clj"}]]

(fact "after loading the hotgym data, it has 87840 items"
      (def hotgym-config {:file "resources/hotgym.csv"
                          :read-n-records :all
                          :fields ["gym" {:type :string
                                          :doc "Name of this Gym"
                                          :encoder {:type :hash-encoder
                                                    :bits 32
                                                    :on 8}
                                          :sequence-flag? true}
                                   "address" {:type :string
                                              :doc "Address of this Gym"
                                              :encoder {:type :hash-encoder
                                                        :bits 32
                                                        :on 8}}
                                   "timestamp" {:type :datetime
                                                :doc "Timestamp of this data record"
                                                :subencode [{:field :day-of-year}
                                                            {:field :day-of-week}
                                                            {:field :time-of-day}
                                                            {:field :weekday?}]}
                                   ]})
      (def hotgym (load-opf-file hotgym-config))
      (count (:parsed-data hotgym)) => 87840

      (mapv (comp str first) (nth (:parsed-data hotgym) 10)) =>
      ["Balgowlah Platinum" "Shop 67 197-215 Condamine Street Balgowlah 2093" "2010-07-02T02:30:00.000Z" "1.2"]

      (def encs (:encoders hotgym))


      (def enc-10 (data-encode hotgym 10))
      (def enc-11 (data-encode hotgym 11))

      (def enc-30 (data-encode hotgym 30))
enc-10 => #{8 12 13 14 15
            42 46 56 64 71 80 85 88
            96 99 101 102 104 118 123 124 130 136 141 145 163
            175 176 189 191 201 204 209 210 221 229 230 236
            237 239 242 246 287 288 289 290 291 292 293 294
            295 296 297 298 299 300 301 302 303 304 305 306
            307 429 430 431 432 433 434 435 436 437 438 439
            440 441 442 443 444 445 446 447 448 449 1348 1349
            1350 1357 1359 1360 1367 1375 1383 1393 1394 1395
            1401 1414 1425 1440 1443 1450 1453 1462 1466}

(difference enc-10 enc-11) => #{429 430 431 432 433 434 435 436 437 438}

(difference enc-10 enc-30) =>  #{429 430 431 432 433 434 435 436 437 438
                                 439 440 441 442 443 444 445 446 447 448 449
                                 1348 1349 1350 1359 1360 1375 1383 1393 1394
                                 1395 1401 1414 1425 1440 1443 1462 1466}

      ;(:bits hotgym) => 1024

      (def uri "datomic:free://localhost:4334/patches")
      (def conn (d/connect uri))
      (def uuid (d/squuid))
      (def sdr-tx-10 (s->db/sdr->tx uuid 10 enc-10))
      @(d/transact conn sdr-tx-10)
      (def sdr-10 (s->db/input-sdr (d/db conn) uuid 10))
      sdr-10 => enc-10
      (def sdr-tx-30 (s->db/sdr->tx uuid 30 enc-30))
      )

#_(write-edn-file hotgym "resources/hotgym.edn")
