(ns clortex.viz.core
  (:require [quil.core :refer :all]
            [datomic.api :as d]
            [clortex.domain.patch.persistent-patch :as patch]
            [clortex.utils.math :refer :all]))

(def uri "datomic:free://localhost:4334/patches")
(def conn (d/connect uri))

(defn neurons
  []
  (let [ctx {:conn conn}
        patch (ffirst (patch/find-patch-uuids ctx))
        neuron-data (patch/find-neuron-ids ctx patch)]
    (mapv #(d/entity (d/db conn) (first %)) neuron-data)))

(defn coords
  [i n-cells]
  (let [rows (int (Math/sqrt n-cells))
        scale (/ (height) rows 1.2)
        x (int (+ 20 (* scale (int (rem i rows)))))
        y (int (+ 20 (* scale (int (/ i rows)))))]
        [(* 2.0 x) y]))

(defn part-line
  [x y x1 y1 fraction]
  (line x y
        (+ x (* fraction (- x1 x)))
        (+ y (* fraction (- y1 y)))))

(defn draw-synapse
  [from-i n-cells synapse post-neuron]
  (let [permanence (:synapse/permanence synapse)
        permanence-threshold (:synapse/permanence-threshold synapse)
        i (:neuron/index post-neuron)
        connected? (>= permanence permanence-threshold)
        [x y] (coords i n-cells)
        [x2 y2] (coords from-i n-cells)]
    (do
      (stroke-weight 1.0)
      (stroke (if connected? 120 64))
      (part-line x2 y2 x y permanence)
      (part-line x y x2 y2 (/ permanence 10.0)))
    (if connected?
      (do
        (stroke 90 (* permanence 255) 255)
        (stroke-weight 1.0)
        (line x2 y2 x y)
        (when (:neuron/active? post-neuron)
          (stroke 90 200 125)
          (line x2 y2 x y))
        (stroke-weight 2.0)
        (stroke 120)
        (part-line x2 y2 x y permanence)))
      ))

(defn draw-distals
  [i n-cells distals]
  (doall (for [distal distals
        synapse (:dendrite/synapses distal)]
    (let [from-neuron (:synapse/pre-synaptic-neuron synapse)
          to-neuron (:synapse/post-synaptic-neuron synapse)
          from-i (:neuron/index from-neuron)
          permanence (:synapse/permanence synapse)
          permanence-threshold (:synapse/permanence-threshold synapse)
          connected? (>= permanence permanence-threshold)
          active? (:neuron/active? from-neuron)
          predictive? (:neuron/active? to-neuron)
          [x y] (coords i n-cells)
          [x2 y2] (coords from-i n-cells)]
      ;(println "i" i "at (" x "," y ") to" from-i "at (" x2 "," y2 ")")
      (if active?
        (do
          (stroke-weight 1.0)
          (stroke (if connected? 120 64))
          (part-line x2 y2 x y permanence)
          (part-line x y x2 y2 (/ permanence 10.0))))
      (if predictive?
        (do
          (stroke 127 255 127)
          (stroke-weight 1.0)
          (stroke (if connected? 120 64))
          (part-line x2 y2 x y permanence)
          (part-line x y x2 y2 (/ permanence 10.0))))
      (if (and active? connected?)
        (do
          (stroke 90 (* permanence 255) (if connected? 255 64))
          (stroke-weight 1.0)
          (line x2 y2 x y)
          (stroke-weight 2.0)
          (stroke (if connected? 120 64))
          (part-line x2 y2 x y permanence)))
      ))))

(defn draw-axons
  [i n-cells conn]
  (let [db (d/db conn)
        targets (d/q '[:find ?synapse ?post
                       :in $ ?i
                       :where
                       [?pre :neuron/index ?i]
                       [?post :neuron/distal-dendrites ?dendrite]
                       [?dendrite :dendrite/synapses ?synapse]
                       [?synapse :synapse/pre-synaptic-neuron ?pre]]
                     db
                     i)]
    ;(println "cell " i " enervates" (count targets) "cells")
    (doall (for [[synapse-id post-neuron-id] targets]
             #_(println "target of i" i "is" target)
             (let [synapse (d/entity db synapse-id)
                   post-neuron (d/entity db post-neuron-id)]
               (draw-synapse i n-cells synapse post-neuron))))))

(defn change-activations
  [cells]
  (let [current-pattern (mapv :neuron/active? cells)
        on-bits (count (filter true? current-pattern))
        target (/ (count cells) 40)
        active? #(>= target (random (count cells)))
        txs (vec (for [neuron cells]
             {:db/id (:db/id neuron) :neuron/active? (active?)}))]
    (d/transact conn txs)
    (neurons)
    ;(println current-pattern)
    ))

(defn setup []
  (set-state! :randomer (random-fn-with-seed 123456))
  (smooth)                          ;; Turn on anti-aliasing
  (frame-rate 30)                   ;; Set framerate to 1 FPS
  (background 33))                  ;; Set the background colour to
                                    ;; a nice shade of grey.

(defn choose-from-active
  [ctx cells]
  (let [randomer (:randomer ctx)
        n-cells (count cells)
        active-cells (vec (filter :neuron/active? cells))
        n-active (count active-cells)
        chosen-active (if (pos? n-active) (randomer n-active) (randomer n-cells))
        chosen-cells (if (pos? n-active) active-cells cells)
        chosen-neuron (get chosen-cells chosen-active)]
        (:neuron/index chosen-neuron)))

(defn draw []
  (let [randomer (state :randomer)
        ctx {:conn conn :randomer randomer}
        patch (ffirst (patch/find-patch-uuids ctx))
        cells (neurons)
        n-cells (count cells)
        rows (int (Math/sqrt n-cells))
        scale (/ (height) rows 1.2)
        diam (inc (int (* 0.2 scale)))
        changer (randomer 5)
        previous-sdr (vec (filter :neuron/active? cells))
        previously-active (choose-from-active ctx cells)
        cells (if (zero? changer) (change-activations cells) cells)
        new-sdr (vec (filter :neuron/active? cells))
        newly-active (if (zero? changer) (choose-from-active ctx cells) (randomer n-cells))
        active-cells (vec (filter :neuron/active? cells))
        n-active (count active-cells)
        ;_ (println "cells:" (count cells) "active:" active-cells)
        chosen-active (if (pos? n-active) (randomer n-active) (randomer n-cells))
        chosen-cells (if (pos? n-active) active-cells cells)
        chosen-neuron (get chosen-cells chosen-active)
        connect-from (:neuron/index chosen-neuron)
        connect-to (randomer n-cells)]
    (background (if (zero? changer) 0 44))

    (if (and
         (zero? (randomer 1))
         (not= newly-active previously-active))
      (do
        ;(println "connecting" connect-from "to" connect-to)
        (patch/connect-distal ctx patch previously-active newly-active)))

    #_(doseq [cell cells]
      (let [i (:neuron/index cell)
            distals (:neuron/distal-dendrites cell)
            fill-color (if (:neuron/active? cell) 255 66)
            [x y] (coords i n-cells)]
        ;(println "i" i "at (" x "," y ")")
        (stroke (randomer 64) (randomer 64) (randomer 64))             ;; Set the stroke colour to a random grey
        (stroke-weight 1)       ;; Set the stroke thickness randomly
        (fill (randomer (+ 100 (count distals))))               ;; Set the fill colour to a random grey
        (draw-distals i n-cells distals)
        ;(no-loop)
        ))
    (doseq [cell previous-sdr]
      (let [i (:neuron/index cell)
            ]
        (draw-axons i n-cells conn)
        ))
    (doseq [cell new-sdr]
      (let [i (:neuron/index cell)
            ]
        (draw-axons i n-cells conn)
        ))
    (doseq [cell cells]
      (let [i (:neuron/index cell)
            fill-color (if (:neuron/active? cell) 255 66)
            [x y] (coords i n-cells)]
        ;(println "i" i "at (" x "," y ")")
        (stroke 127)             ;; Set the stroke colour to a random grey
        (stroke-weight 0.3)       ;; Set the stroke thickness randomly
        (fill fill-color 66 66)               ;; Set the fill colour to a random grey
        (rect x y diam diam)
        ))
    (doseq [cell previous-sdr]
      (let [i (:neuron/index cell)
            fill-color 195
            [x y] (coords i n-cells)]
        ;(println "i" i "at (" x "," y ")")
        (stroke 127)             ;; Set the stroke colour to a random grey
        (stroke-weight 0.3)       ;; Set the stroke thickness randomly
        (fill fill-color 180 180)               ;; Set the fill colour to a random grey
        (rect x y diam diam)
        ))
    #_(println (count cells) "\tcells"
             (count (filter :neuron/active? cells)) "\tactive"
             )))

(defsketch example                  ;; Define a new sketch named example
  :title "Clortex Visualisation"    ;; Set the title of the sketch
  :setup setup                      ;; Specify the setup fn
  :draw draw                        ;; Specify the draw fn
  :size [800 400])                  ;; You struggle to beat the golden ratio

