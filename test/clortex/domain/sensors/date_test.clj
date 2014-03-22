(ns clortex.domain.sensors.date-test
  (:use midje.sweet)
  (:use clortex.domain.sensors.date))

[[:subsection {:title "OPF Date Parsing"}]]

(fact
	(str (parse-opf-date "2010-07-02 08:15:00.01")) => "2010-07-02T08:15:00.010Z"
)
	

