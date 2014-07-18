(ns clortex.date-encoder-test
  (:use midje.sweet)
  (:require [clortex.domain.encoders.date-time :as d]
            [clortex.domain.encoders.scalar :as s]
            [clortex.domain.encoders.rdse :as r]
            [clortex.utils.math :refer :all]
            [clojure.set :refer [difference intersection]]
            [clj-time.core :as t]))

[[:chapter {:title "Background"}]]
"
Time is enormously important to both the neocortex and machine intelligence, so
NuPIC and Clortex must have rich capabilities for handling and encoding temporal
data values.

NuPIC has a simple date-time encoder
[source code](https://github.com/numenta/nupic/blob/master/nupic/encoders/date.py)
which optionally encodes the following sub-encodings:

* season (season of the year; units = day):
* dayOfWeek (monday = 0; units = day)
* weekend (boolean: 0, 1)
* holiday (boolean: 0, 1)
* timeOfday (midnight = 0; units = hour)
* customDays (set certain weekdays as 1, others are 0)
"
(facts
 (def thursday-17 (t/date-time 2014 7 17 13 45 30))
 (def friday-18 (t/date-time 2014 7 18 13 25 30))
 (def enc (d/opf-date-encoder))
 (def time-sdr (:encode enc))
 (time-sdr thursday-17) => #{25 26 27 28 29 30 31 32 33 34
                             35 36 37 38 39 40 41 42 43 44 45
                             645 646 647 648 649 650 651 652 653 654
                             655 656 657 658 659 660 661 662 663 664 665}
 (time-sdr friday-18)   => #{33 34 35 36 37 38 39 40 41 42
                             43 44 45 46 47 48 49 50 51 52 53
                             632 633 634 635 636 637 638 639 640 641
                             642 643 644 645 646 647 648 649 650 651 652}

 (intersection (time-sdr thursday-17) (time-sdr friday-18))
 => #{33 34 35 36 37 38 39 40 41 42 43 44 45 645 646 647 648 649 650 651 652}

 (def wenc (d/weekday-encoder 70 10))
 ((:encode wenc) thursday-17) => #{30 31 32 33 34 35 36 37 38 39}
 (def tenc (d/time-of-day-encoder 1024 21))
 ((:encode tenc) thursday-17) => #{575 576 577 578 579 580 581 582 583 584
                                   585 586 587 588 589 590 591 592 593 594 595}
)

[[:section {:title "Atomic Temporal Encoders"}]]


[[:subsection {:title "Basic Random Distributed Scalar Encoder - Implementation"}]]
"
The following is a basic RDSE implemented in Clojure. It starts off empty, and adds buckets lazily as new
values are encoded. **Please let me know if there is anything unclear in this code!**.
"
[[:file {:src "src/clortex/domain/encoders/date_time.clj"}]]

[[:subsection {:title "Testing the RDSE on 4-of-12-bit encoding"}]]
"Let's re-run the tests for the simple 12 bit encoder, where we had very quickly saturated the
basic `scalar-encoder`."

