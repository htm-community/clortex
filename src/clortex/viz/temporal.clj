(ns clortex.viz.temporal
  (:require [quil.core :refer :all]
            [clortex.domain.patch.pure-patch :as patch]
            [clortex.utils.math :refer :all]))

(defn key-press []
  (background (rand 255)))

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
        (reset! (state :stepping?) true))))
  (background (rand 255)))

(defn setup []
  (let [randomer (random-fn-with-seed 123456)
        ctx {:randomer randomer}
        fonts (available-fonts)
        message-font (create-font "Verdana" 10 true)]
   (set-state! :randomer randomer
               :ctx ctx
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

(defn draw []
  (background 0)
  (ellipse 400 400 100 100))
