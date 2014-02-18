(defproject clortex "0.1.1-SNAPSHOT"
  :description "** Clortex: Implementation in Clojure of Jeff Hawkins' Hierarchical Temporal Memory 
      & Cortical Learning Algorithm.
      **Warning: Pre-alpha code.**
	This project has just begun as is under daily development. Anything and everything is likely to change drastically without a moment's notice.
	"
  :url "https://github.com/fergalbyrne/clortex"
  :license {:name "GPLv3"
            :url "http://www.gnu.org/licenses/gpl.html"}
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                [criterium "0.4.3"]]}}
  :jvm-opts ^:replace ["-Xmx4g" "-XX:+TieredCompilation" #_"-XX:-TieredStopAtLevel=1" #_"-XX:+PrintCompilation"]
  :repl-options {
                 :init (println "Init")
                 }
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [incanter/incanter-core "1.5.4"]
                 [incanter/incanter-io "1.5.4"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojure/data.json "0.2.1"]
                 [enlive "1.1.5"]
                 [clojure-opennlp "0.3.2"]
                 [clojurewerkz/buffy "1.0.0-beta1"]
                 [clj-time "0.6.0"]]
  :plugins [[lein-marginalia "0.7.1"]]
  :documentation {:files {"doc/index"
                          {:input "test/clortex/core_test.clj"
                           :title "clortex"
                           :sub-title "Clojure Library for Jeff Hawkins' Hierarchical Temporal Memory"
                           :author "Fergal Byrne"
                           :email  "fergalbyrnedublin@gmail.com"
                           :tracking "UA-44409012-2"}
						"doc/rdse"
                          {:input "test/clortex/rdse_test.clj"
                           :title "Random Distributed Scalar Encoder"
                           :sub-title "Experiments on improving the Scalar Encoder"
                           :author "Fergal Byrne"
                           :email  "fergalbyrnedublin@gmail.com"
                           :tracking "UA-44409012-2"}
						  "doc/encoders"
                          {:input "test/clortex/domain/encoders/core_test.clj"
                           :title "clortex encoders"
                           :sub-title "Encoding Values into CLA SDRs"
                           :author "Fergal Byrne"
                           :email  "fergalbyrnedublin@gmail.com"
                           :tracking "UA-44409012-2"}
						  "doc/patch"
                          {:input "test/clortex/domain/patch/patch_test.clj"
                           :title "clortex patch"
                           :sub-title "Primary Implementation of CLA"
                           :author "Fergal Byrne"
                           :email  "fergalbyrnedublin@gmail.com"
                           :tracking "UA-44409012-2"}
						"doc/sensors"
                          {:input "test/clortex/domain/sensors/core_test.clj"
                           :title "clortex sensors"
                           :sub-title "Bringing Data into the CLA"
                           :author "Fergal Byrne"
                           :email  "fergalbyrnedublin@gmail.com"
                           :tracking "UA-44409012-2"}
                          }}
  :eval-in-leiningen true)

