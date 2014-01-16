(ns clortex.core)
(use 'incanter.core
     'incanter.io)

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

(read-dataset "resources/small-sample.csv")

