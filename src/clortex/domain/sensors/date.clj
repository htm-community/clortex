(ns clortex.domain.sensors.date)
(require 'clj-time.core)
	
(def opf-timestamp-re #"(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):([0-9.]+)")
(defn strip-leading-zeros [s] (clojure.string/replace-first s #"^0+([1-9])" "$1"))

(defn parse-opf-date
	[s]
	(let [m (re-matches opf-timestamp-re s)]
	  (if m (let [rev (reverse (map strip-leading-zeros (rest m)))
                  secs (java.lang.Double/parseDouble (first rev))
                  items (map #(. Integer parseInt %) (rest rev))
                  ]
        (apply clj-time.core/date-time (reverse (conj items secs)))))))
		
