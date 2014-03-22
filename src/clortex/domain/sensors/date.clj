(ns clortex.domain.sensors.date
	(require [clj-time.core :as tc]
		[clj-time.format :as tf]))

(def opf-timestamp-re #"(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):([0-9.]+)")
(defn strip-leading-zeros [s] (clojure.string/replace-first s #"^0+([1-9.])" "$1"))

(defn old-parse-opf-date
	[s]
	(let [m (re-matches opf-timestamp-re s)]
	  (if m (let [rev (reverse (map strip-leading-zeros (rest m)))
                  secs (java.lang.Double/parseDouble (first rev))
                  items (map #(. Integer parseInt %) (rest rev))
                  ]
        (apply tc/date-time (reverse (conj items secs)))))))

(def opf-format (tf/formatter "yyyy-MM-dd HH:mm:ss.SS"))
;(tf/parse opf-format "16:13:49:06 on 2013-04-06")
(defn parse-opf-date [s] (tf/parse opf-format s))		
