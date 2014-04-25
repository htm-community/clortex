(ns clortex.viz.core
  (:require [quil.core :refer :all]
            [datomic.api :as d]
            [clortex.domain.patch.persistent-patch :as patch]
            [clortex.utils.math :refer :all]))

(def uri "datomic:free://localhost:4334/patches")

(defn neurons
  ([] (neurons (state :ctx) (state :patch-id)))
  ([ctx patch]
     (let [conn (ctx :conn)]
    (patch/neuron-entities (d/db conn) patch))))

(memoize (defn coords
  [i n-cells]
  (let [rows (int (Math/sqrt n-cells))
        scale (/ (height) rows 1.2)
        x (int (+ 20 (* scale (int (rem i rows)))))
        y (int (+ 20 (* scale (int (/ i rows)))))]
        [(* 2.0 x) y])))


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
          (stroke 90 200 125 (* 3 alpha))
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

(defn draw-connected-axons-for
  [cells n-cells db]
  (doall (for [[from to-neurons synapses] (map #(patch/enervated-by-connected db %) cells)]
           (do #_(println (format "from %11d: [%4d] neurons" (:neuron/index from) (count (mapv identity to-neurons))))
    (doall (map #(let [synapse (d/entity db %2)
                   post (d/entity db %1)]
            #_(println "neuron " (:neuron/index post))
               (draw-synapse synapse post n-cells))
         to-neurons synapses))))))


(defn change-activations
  [cells]
  (let [start (.. System currentTimeMillis)
        n-cells (state :n-cells)
        old-ons @(state :current-sdr)
        _ (reset! (state :previous-sdr) old-ons)
        ;_ (println "new-ons:" old-ons)
        target (/ n-cells (* 40 16))
        active? (fn [_] (>= target (random n-cells)))
        new-cells (into [] (take target (repeatedly #(rand-nth cells))))
        new-ids (mapv :db/id new-cells)
        new-ons (into [] new-cells)
        ;_ (println "new-ons:" new-ons)
        on-txs (mapv #(assoc {} :db/id % :neuron/active? true) new-ids)
        ;_ (println "on-txs:" on-txs)
        off-cells (vec (clojure.set/difference (set (mapv :db/id old-ons)) (set new-ids)))
        off-txs (mapv #(assoc {} :db/id % :neuron/active? false) off-cells)
        ;_ (println "off-txs:" off-txs)
        txs (concat off-txs on-txs)
        end-let (.. System currentTimeMillis)]
    @(d/transact ((state :ctx) :conn) txs)
    (reset! (state :current-sdr) new-ons)
    (println (format "%5d - changed activations %5d txs %4dms + %4dms" (frame-count) (count txs)
                     (- end-let start) (- (.. System currentTimeMillis) end-let)))))

(defn setup []
  (let [conn (d/connect uri)
        randomer (random-fn-with-seed 123456)
        ctx {:conn conn :randomer randomer}
        patch (ffirst (patch/find-patch-uuids ctx))
        patch-id (ffirst (patch/find-patch ctx patch))
        _ (println "patch" patch-id)
        cells (neurons ctx patch-id)
        n-cells (count cells)
        rows (int (Math/sqrt n-cells))
        scale (/ (height) rows 1.2)
        fonts (available-fonts)
        message-font (create-font "Verdana" 10 true)]
   (set-state! :randomer randomer
               :ctx ctx
               :patch patch
               :patch-id patch-id
               :n-cells n-cells
               :rows rows
               :scale scale
               :diam (inc (int (* 0.2 scale)))
               :previous-sdr (atom [])
               :current-sdr (atom (filterv :neuron/active? cells))
               :paused? (atom false)
               :stepping? (atom false)
               :show-synapses? (atom false)
               :learning? (atom false)
               :updating? (atom true)
               :key-pressed (atom nil)
               :anim-frames (atom 0)
    )
  (smooth)
  (frame-rate 60)
  (background 33)

    (text-font message-font)
    (hint :enable-native-fonts)))

(defn key-press []
  (let [raw (raw-key)]
    (println (str "Key pressed:" raw))
	  (reset! (state :key-pressed) raw)
    (case raw
      \space (swap! (state :paused?) not)
      \. (reset! (state :stepping?) true)
      \s (swap! (state :show-synapses?) not)
      \l (swap! (state :learning?) not)
      \u (swap! (state :updating?) not)
      (do
        (reset! (state :stepping?) true))
  )))

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

(defn patch-distance
  [to from n-cells]
  (let [[x y] (coords (:neuron/index from) n-cells)
        [x1 y1] (coords (:neuron/index to) n-cells)
        xdist (- x1 x)
        ydist (- y1 y)]
    (Math/sqrt (+ (* xdist xdist) (* ydist ydist)))))

(defn add-connections
  [n]
  (let [start (.. System currentTimeMillis)
        randomer (state :randomer)
        ctx (state :ctx)
        conn (:conn ctx)
        patch (state :patch)
        n-cells (state :n-cells)
        ;cells (neurons)
        previous-sdr @(state :previous-sdr)
        current-sdr @(state :current-sdr)

        txs (for [newly-active current-sdr
                  previously-active (filterv (fn [_] (zero? (randomer 2))) previous-sdr)
                  :let [dist (patch-distance previously-active newly-active n-cells)]
                  :when (and (zero? (randomer (inc (* 0.01 n dist))))
                             (not (nil? previously-active))
                             (not= newly-active previously-active))]
              (patch/connect-distal-tx ctx previously-active newly-active))
        end-let (.. System currentTimeMillis)]
    (d/transact-async conn (apply concat txs))
    (println (format "%5d - add connections %5d txs %4dms + %4dms" (frame-count) (count txs)
                     (- end-let start) (- (.. System currentTimeMillis) end-let)))))

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
        changer (if (< (frame-count) 2) 0 (randomer 5))
        anim-frames @(state :anim-frames)
        max-anim-size 4
        changer? (if (and
                      (>= anim-frames (+ max-anim-size 3))
                      @(state :updating?)
                      (not @(state :paused?)))
                   true false)
        _ (if changer?
            (reset! (state :anim-frames) 0)
            (swap! (state :anim-frames) inc))
        anim-size (if (< anim-frames max-anim-size) anim-frames (max 0 (- max-anim-size anim-frames)))
        cells (if changer? (change-activations cells) cells)
        previous-sdr @(state :previous-sdr)
        new-sdr @(state :current-sdr)
        _ (if (and changer? @(state :learning?))
            (add-connections 20))
        cells (if changer? (neurons) cells)
        db (d/db conn)
        message-y (- (height) 30)
        speedo-y (- (height) 10)
        end-let (.. System currentTimeMillis)]
    (if @(state :show-synapses?)
      (do (draw-connected-axons-for previous-sdr n-cells db)
          (draw-connected-axons-for new-sdr n-cells db)))
    (fill 127 255 127 225)
    (stroke 127 255 127 225)
    (text-size 10)
    (text (format "Frame: %d" (frame-count)) 50 message-y)
    (text (format "Prep: %dms" (- end-let start)) 120 message-y)
    (if @(state :show-synapses?)
      (text (format "Syn: %dms" (- (.. System currentTimeMillis) end-let)) 200 message-y))
    (stroke 127)
    (stroke-weight 0.3)
    (fill 33 66 66 222)
    #_(let [diam (/ diam 2.0)]
      (doseq [i (range n-cells)]
        (let [[x y] (coords i n-cells)]
          (rect x y diam diam))))
    (stroke 127)
    (stroke-weight 0.3)
    (fill 195 180 180 (* 40 (- max-anim-size anim-size)))
    (doseq [cell previous-sdr]
      (let [i (:neuron/index cell)
            [x y] (coords i n-cells)
            diam (* (- max-anim-size anim-size 1) diam)]
        (ellipse x y diam diam)))
    (stroke 127)             ;; Set the stroke colour to a random grey
    (stroke-weight 0.3)       ;; Set the stroke thickness randomly
    (fill (* anim-size 80) 95 95 (* (inc anim-size) 75))               ;; Set the fill colour to a random grey
    (doseq [cell new-sdr]
      (let [i (:neuron/index cell)
            [x y] (coords i n-cells)
            diam (* (+ anim-size 1) diam)]
        (ellipse x y diam diam)))
    (fill 127 255 127 225)
    (stroke 127 255 127 225)
    (text-size 10)
    (text (format "Rendered: %4dms" (- (.. System currentTimeMillis) end-let))
          480 message-y)
    (when @(state :learning?)
      (text "Learning: ON" 300 message-y))
        (stroke 127 255 127)
    (fill 127 255 127 (* 40 (- max-anim-size anim-size)))
    (stroke 127 255 127 127)
    (rect 5 speedo-y (- end-let start) 3)
    (fill 127 127 255 (* 40 (- max-anim-size anim-size)))
    (rect (+ 5 (- end-let start)) speedo-y (- (.. System currentTimeMillis) end-let) 3)
    (if changer?
      (do
        (fill 127 255 127)
        (stroke 127 255 127)
        (rect 5 speedo-y (- end-let start) 3)))
    (let [total-time (- (.. System currentTimeMillis) start)
          render-time (- (.. System currentTimeMillis) end-let)
          prep-time (- end-let start)
          fps (/ 1000.0 total-time)]
    (stroke 127 255 127 225)
    (fill 127 255 127 225)
    (fill (- 255 (* (- fps 60) 4))
          (+ 127 (* fps 5))
          127
          (* fps 8))
    (text (format "%3.1fFPS" fps)
          15 15)
    (no-stroke)
    #_(ellipse 30 50 fps fps))
    ))

