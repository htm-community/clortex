(ns clortex.utils.datomic
  (:require [datomic.api :as d]
            [adi.core :as adi]))

(def clortex-schema
  {:patch   {:type    [{:type :keyword}]
             :name    [{:type :string}]
             :uuid    [{:type :uuid}]
             :timestep [{:type :long :default 0}]
             :columns [{:type :ref
                        :index true
                        :ref  {:ns   :column
                               :rval :patch}
                        :cardinality :many}]
             :neurons [{:type :ref
                        :index true
                        :ref  {:ns   :neuron
                               :rval :patch}
                        :cardinality :many}]
             :inputs   [{:type :ref
                         :index true
                         :ref  {:ns   :neuron
                                :rval :patch}
                         :cardinality :many}]}
   :column  {:type    [{:type :keyword}]
             :index   [{:type :long}]
             :neurons [{:type :ref
                        :ref  {:ns   :neuron
                               :rval :column}
                        :cardinality :many}]}
   :neuron  {:index                 [{:type :long}]
             :feedforward-potential [{:type :long
                                      :default 0}]
             :prediction-potential  [{:type :long
                                      :default 0}]
             :active?               [{:type :boolean
                                      :default false}]
             :proximal-dendrite     [{:type :ref
                                      :ref  {:ns :dendrite
                                             :rval :neuron}
                                      :cardinality :one}]
             :distal-dendrites      [{:type :ref
                                      :index true
                                      :ref  {:ns :dendrite
                                             :rval :neuron}
                                      :cardinality :many}]}
   :dendrite {:type     [{:type :enum
                          :default :distal
                          :enum {:ns :dendrite.type
                                 :values #{:distal :proximal :input :output}}}]
              :capacity [{:type :long
                          :default 32}]
              :threshold [{:type :long
                           :default 16}]
              :active?  [{:type :boolean
                          :default false}]
              :synapses [{:type :ref
                          :index true
                          :ref   {:ns   :synapse
                                  :rval :dendrite}
                          :cardinality :many}]}
   :synapse {:type       [{:type :enum
                           :default :excitatory
                           :enum {:ns :synapse.type
                                  :values #{:excitatory :inhibitory :io}}}]
             :permanence [{:type :double
                           :default 0}]
             :permanence-threshold [{:type :double :default 0.2}]
             :pre-synaptic-neuron  [{:type :ref
                                     :index true
                                     :ref   {:ns   :neuron
                                             :rval :fanout}
                                     :cardinality :one}]}
   :sensor-data {:timestep [{:type :long :default 0 :index true}]
                 :sdr [:type :long :cardinality :many]}
   })

