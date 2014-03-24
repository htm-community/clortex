(ns clortex.domain.sensors.core-test
  (:use midje.sweet)
  (:use clortex.domain.sensors.core))

[[:chapter {:title "Sensors"}]]

"Sensors gather infortmation from the world and deliver it in encoded form to the CLA.

"

[[:section {:title "Numenta OPF Sensor (CSV data)"}]]

"The first sensor in `clortex` reads CSV data which is compatible with Numenta's OPF
(Online Prediction Framework) software."

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

(def hotgym (load-opf-file "resources/hotgym.csv"))

#_(write-edn-file hotgym "resources/hotgym.edn")
