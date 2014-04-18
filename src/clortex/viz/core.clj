(ns clortex.viz.core
  (:require [quil.core :refer :all]
            [datomic.api :as d]
            [clortex.domain.patch.persistent-patch :as patch]
            [clortex.utils.math :refer :all]))

(def uri "datomic:free://localhost:4334/patches")

(defn neurons
  ([] (neurons (state :ctx)))
  ([ctx]
     (let [conn (ctx :conn)
        patch (ffirst (patch/find-patch-uuids ctx))
        neuron-data (patch/find-neuron-ids ctx patch)]
    (mapv #(d/entity (d/db conn) (first %)) neuron-data))))

(defn coords
  [i n-cells]
  (let [rows (int (Math/sqrt n-cells))
        scale (/ (height) rows 1.2)
        x (int (+ 20 (* scale (int (rem i rows)))))
        y (int (+ 20 (* scale (int (/ i rows)))))]
        [(* 2.0 x) y]))


(defn part-line-direct
  [x y x1 y1 fraction]
  (line x y
        (+ x (* fraction (- x1 x)))
        (+ y (* fraction (- y1 y)))))

(defn part-line
  [x y x1 y1 fraction]
  (let [wrap-x? (> (Math/abs (- x1 x)) (/ (width) 2.0))
        wrap-y? (> (Math/abs (- y1 y)) (/ (height) 2.0))
        ]
    (if (or wrap-x? wrap-y?)
      (let [x2 (if wrap-x?
                 (- x1 (width))
                 x1)
            y2 (if wrap-y?
                 (- y1 (height))
                 y1)
            x3 (if wrap-x?
                 (+ x (width))
                 x)
            y3 (if wrap-y?
                 (+ y (height))
                 y)]
        (part-line-direct x y x2 y2 fraction)
        (part-line-direct x3 y3 x1 y1 fraction))
      (part-line-direct x y x1 y1 fraction))))

(defn draw-synapse
  [synapse post-neuron n-cells]
  (let [pre-neuron (:synapse/pre-synaptic-neuron synapse)
        permanence (:synapse/permanence synapse)
        permanence-threshold (:synapse/permanence-threshold synapse)
        i (:neuron/index post-neuron)
        from-i (:neuron/index pre-neuron)
        connected? (>= permanence permanence-threshold)
        [x y] (coords i n-cells)
        [x2 y2] (coords from-i n-cells)
        stroke-gray (if connected? 120 64)
        alpha 22]
    (do
      (stroke-weight 0.5)
      (stroke stroke-gray stroke-gray stroke-gray alpha)
      (part-line x2 y2 x y permanence)
      (part-line x y x2 y2 (/ permanence 10.0)))
    (if connected?
      (do
        (stroke 90 (* permanence 155) 127 alpha)
        (stroke-weight 1.0)
        (part-line x2 y2 x y 1.0)
        (when (:neuron/active? post-neuron)
          (stroke 90 200 125 alpha)
          (line x2 y2 x y))
        (stroke-weight 2.0)
        (stroke stroke-gray stroke-gray stroke-gray alpha)
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
                 [x2 y2] (coords from-i n-cells)
                 stroke-gray (if connected? 120 64)
                 alpha 11]
             ;(println "i" i "at (" x "," y ") to" from-i "at (" x2 "," y2 ")")
             (if active?
               (do
                 (stroke-weight 0.5)
                 (stroke stroke-gray stroke-gray stroke-gray alpha)
                 (part-line x2 y2 x y permanence)
                 (part-line x y x2 y2 (/ permanence 10.0))))
             (if predictive?
               (do
                 (stroke 127 255 127 alpha)
                 (stroke-weight 0.5)
                 (stroke stroke-gray stroke-gray stroke-gray alpha)
                 (part-line x2 y2 x y permanence)
                 (part-line x y x2 y2 (/ permanence 10.0))))
             (if (and active? connected?)
               (do
                 (stroke 90 (* permanence 127) (if connected? 127 64) alpha)
                 (stroke-weight 0.5)
                 (line x2 y2 x y)
                 (stroke-weight 0.75)
                 (stroke stroke-gray stroke-gray stroke-gray alpha)
                 (part-line x2 y2 x y permanence)))
             ))))

(defn draw-axons
  [i n-cells db]
  (let [targets (d/q '[:find ?synapse ?post
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
             (let [synapse (d/entity db synapse-id)
                   post (d/entity db post-neuron-id)]
               (draw-synapse synapse post n-cells))))))

(defn draw-axons-for
  [cells n-cells db]
  (let [targets (d/q '[:find ?synapse ?post
                       :in $ [?pre]
                       :where
                       [?synapse :synapse/pre-synaptic-neuron ?pre]
                       [?post :neuron/distal-dendrites ?dendrite]
                       [?dendrite :dendrite/synapses ?synapse]]
                     db
                     (mapv :db/id cells))]
    ;(println "cell " i " enervates" (count targets) "cells")
    (doall (for [[synapse-id post-neuron-id] targets]
             #_(println "target of i" i "is" target)
             (let [synapse (d/entity db synapse-id)
                   post (d/entity db post-neuron-id)]
               (draw-synapse synapse post n-cells))))))

(defn draw-axons-for
  [cells n-cells db]
  (doall (for [[from to-neurons synapses] (map #(patch/enervated-by db %) cells)]
           (do #_(println (format "from %11d: [%4d] neurons" (:neuron/index from) (count (mapv identity to-neurons))))
    (doall (map #(let [synapse (d/entity db %2)
                   post (d/entity db %1)]
            #_(println "neuron " (:neuron/index post))
               (draw-synapse synapse post n-cells))
         to-neurons synapses))))))


(defn change-activations
  [cells]
  (let [current-pattern (mapv :neuron/active? cells)
        _ (reset! (state :previous-sdr) (filterv :neuron/active? cells))
        on-bits (count (filter true? current-pattern))
        target (/ (count cells) 40)
        active? #(>= target (random (count cells)))
        txs (vec (for [neuron cells]
             {:db/id (:db/id neuron) :neuron/active? (active?)}))]
    #_(println "previous:" (state :previous-sdr))
    @(d/transact ((state :ctx) :conn) txs)
    (neurons)
    ))

(defn setup []
  (let [conn (d/connect uri)
        randomer (random-fn-with-seed 123456)
        ctx {:conn conn :randomer randomer}
        n-cells (count (neurons ctx))
        rows (int (Math/sqrt n-cells))
        scale (/ (height) rows 1.2)]
   (set-state! :randomer randomer
               :ctx ctx
               :patch (ffirst (patch/find-patch-uuids ctx))
               :n-cells n-cells
               :rows rows
               :scale scale
               :diam (inc (int (* 0.2 scale)))
               :previous-sdr (atom []))
    )
  (smooth)
  (frame-rate 30)
  (background 33))

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

(defn choose-from-previous
  [ctx cells]
  (let [randomer (:randomer ctx)
        n-cells (state :n-cells)
        previous-cells @(state :previous-sdr)
        n-previous (count previous-cells)
        chosen-previous (if (pos? n-previous) (randomer n-previous) (randomer n-cells))
        chosen-cells (if (pos? n-previous) previous-cells cells)
        ;_ (println "choosing " chosen-previous "from" n-previous)
        chosen-neuron (get chosen-cells chosen-previous)]
    ;(println "chosen:" chosen-neuron)
        (:neuron/index chosen-neuron)))

#_(defn connect-distal
  [ctx patch-uuid from to async?]
  (when (zero? (count (synapse-between ctx patch-uuid from to)))
   (let [conn (:conn ctx)
        randomer (:randomer ctx)
        patch-id (find-patch-id ctx patch-uuid)
        from-id (find-neuron-id ctx patch-id from)
        to-id (find-neuron-id ctx patch-id to)
        synapse-id (d/tempid :db.part/user)
        permanence-threshold 0.2
        permanent? (> (randomer 3) 0)
        permanence (* permanence-threshold (if permanent? 1.1 0.9))
        synapse-tx {:db/id synapse-id
                    :synapse/pre-synaptic-neuron from-id
                    :synapse/permanence permanence
                    :synapse/permanence-threshold permanence-threshold}
        dendrites (find-dendrites ctx to-id)
        dendrites (if (empty? dendrites)
                    (add-dendrite! ctx to-id)
                    dendrites)
        dendrite (ffirst dendrites)]
    ;(println "Connecting " from-id "->" to-id "Adding synapse" synapse-id "to dendrite" dendrite)
    (if async?
      (d/transact-async conn [{:db/id dendrite :dendrite/synapses synapse-id}
                       synapse-tx])
      @(d/transact conn [{:db/id dendrite :dendrite/synapses synapse-id}
                       synapse-tx])))))

(defn add-connections
  [n]
  (let [randomer (state :randomer)
        ctx (state :ctx)
        conn (:conn ctx)
        patch (state :patch)
        cells (neurons)
        previous-sdr @(state :previous-sdr)
        txs (for [previously-active previous-sdr
                  newly-active (filterv :neuron/active? cells)
                  :when (and (zero? (randomer n))
                             (not (nil? previously-active))
                             (not= newly-active previously-active))]
              (patch/connect-distal-tx ctx previously-active newly-active))]
    (println (count txs) " txs")
    (d/transact-async conn (apply concat txs))))

#_(defn add-connections
  [n]
  (let [randomer (state :randomer)
        ctx (state :ctx)
        conn (:conn ctx)
        patch (state :patch)
        cells (neurons)
        previous-sdr @(state :previous-sdr)]
    (doall
      (for [previously-active previous-sdr
          newly-active (filterv :neuron/active? cells)
          :when (and (zero? (randomer n))
                     (not (nil? previously-active))
                     (not= newly-active previously-active))]
      (do
        (println (frame-count) "connecting" previously-active "to" newly-active)
        (patch/connect-distal ctx patch previously-active newly-active true))))))

(defn draw []
  (background 0)
  (fill 127 255 255)
  (rect 5 35 30 30)
  (let [start (.. System currentTimeMillis)
        randomer (state :randomer)
        ctx (state :ctx)
        conn (:conn ctx)
        patch (state :patch)
        cells (neurons)
        n-cells (state :n-cells)
        rows (state :rows)
        scale (state :scale)
        diam (state :diam)
        changer (if (< (frame-count) 5) 0 (randomer 5))
        cells (if (zero? changer) (change-activations cells) cells)
        previous-sdr @(state :previous-sdr)
        previously-active (choose-from-previous ctx cells)
        new-sdr (vec (filter :neuron/active? cells))
        newly-active (choose-from-active ctx cells)
        active-cells (filterv :neuron/active? cells)
        n-active (count active-cells)
        ;_ (println "cells:" (count active-cells) "active:" active-cells)
        chosen-active (if (pos? n-active) (randomer n-active) (randomer n-cells))
        chosen-cells (if (pos? n-active) active-cells cells)
        chosen-neuron (get chosen-cells chosen-active)
        connect-from (:neuron/index chosen-neuron)
        connect-to (randomer n-cells)
        _ (if (zero? changer) (add-connections 50))
        cells (if (zero? changer) (neurons) cells)
        db (d/db conn)
        end-let (.. System currentTimeMillis)]
    (if (zero? changer)
      (do
        (background 10)
        (fill 127 255 127)
        (stroke 127 255 127)
        (rect 5 5 30 30)))
  (fill 30)
  (rect 5 35 30 30)

    #_(if (and
         (zero? (randomer 1))
         (not (nil? previously-active))
         (not= newly-active previously-active))
      (do
        (println (frame-count) "(" (- end-let start) "ms)" "connecting" previously-active "to" newly-active)
        (patch/connect-distal ctx patch previously-active newly-active true))
      #_(println (frame-count) "(" (- end-let start) "ms)"))

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
    (draw-axons-for previous-sdr n-cells db)
    (draw-axons-for new-sdr n-cells db)
    (println (format "%5d axons drawn %4dms + %4dms" (frame-count) (- end-let start) (- (.. System currentTimeMillis) end-let)))
    (doseq [cell cells]
      (let [i (:neuron/index cell)
            fill-color (if (:neuron/active? cell) 255 33)
            [x y] (coords i n-cells)
            diam (if (:neuron/active? cell) diam (/ diam 3.0))]
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
    (doseq [cell (filter :neuron/active? cells)]
      (let [i (:neuron/index cell)
            fill-color (if (:neuron/active? cell) 255 66)
            [x y] (coords i n-cells)]
        ;(println "i" i "at (" x "," y ")")
        (stroke 127)             ;; Set the stroke colour to a random grey
        (stroke-weight 0.3)       ;; Set the stroke thickness randomly
        (fill fill-color 66 66)               ;; Set the fill colour to a random grey
        (rect x y diam diam)
        ))
    #_(println (format "%5d %4dms %4dms" (frame-count) (- end-let start) (- (.. System currentTimeMillis) end-let)))
    #_(println (count cells) "\tcells"
             (count (filter :neuron/active? cells)) "\tactive"
             )))

