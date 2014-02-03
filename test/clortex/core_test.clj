(ns clortex.core-test
  (:use midje.sweet)
  (:require [clortex.core :refer :all]))

[[:chapter {:title "Introduction"}]]

[[:section {:title "Background"}]]

"**Hierarchical Temporal Memory** (*HTM*) is a theory of the neocortex developed by Jeff Hawkins in the early-mid 2000's. HTM explains the working of the neocortex as a hierarchy of **regions**, each of which performs a similar algorithm. The algorithm performed in each region is known in the theory as the **Cortical Learning Algorithm** (*CLA*).

`clortex` is a new implementation of HTM/CLA in Clojure."

[[:chapter {:title "Requirements"}]]

[[:section {:title "Directly Analogous to HTM/CLA Theory"}]]
"
*In order to be a platform for demonstration, exploration and experimentation of Jeff Hawkins’ theories, the system must at all levels of relevant detail match the theory directly (ie 1:1). Any optimisations introduced may only occur following an effectively mathematical proof that this correspondence is maintained under the change.*

There are several benefits to this requirement. Firstly, during development, this requirement provides a rigid and testable constraint on the options for implementation. With a good model of the theory in his mind, we may proceed with confidence to use transparently analogous data structures and algorithms, leaving the question of computational performance for a later day.

Secondly, this requirement will ensure that the system at its heart remains a working implementation of the theory as it develops. In addition, because of this property, it will be directly usable by Jeff and any co-workers (including us) in extending and experimenting with new ideas in the theoretical space. This will enhance support for the new project, and encourage the HTM community to consider the new project as a parallel or alternative way to realise their own goals.

Thirdly, the software will provide a runnable explanation of the theory, real working code (see next requirement) replacing the pseudocode and providing live imagery instead of diagrams (see later requirement). 

Lastly, we feel that the theory deserves software of similar quality, and that this has slowed the realisation of the goals of all concerned. The development of a true analogue in software will pave the way for a rapid expansion in interest in the entire project. In particular, this will benefit anyone seeking to exploit the commercial advantages which the CLA offers.
"

[[:section {:title "Transparently Understandable Implementation in Source Code"}]]

"*All source code must at all times be readable by a non-developer. This can only be achieved if a person familiar with the theory and the models (but not a trained programmer) can read any part of the source code and understand precisely what it is doing and how it is implementing the algorithms.*

This requirement is again deliberately very stringent, and requires the utmost discipline on the part of the developers of the software. Again, there are several benefits to this requirement.

Firstly, the extreme constraint forces the programmer to work in the model of the domain rather than in the model of the software. This constraint, by being adhered to over the lifecycle of the project, will ensure that the only complexity introduced in the software comes solely from the domain. Any other complexity introduced by the design or programming is known as incidental complexity and is the cause of most problems in software.

Secondly, this constraint provides a mechanism for verifying the first requirement. Any expert in the theory must be able to inspect the code for an aspect of the system and verify that it is transparently analogous to the theory.

Thirdly, anyone wishing to extend or enhance the software will be presented with no introduced obstacles, leaving only their level of understanding of the workings of the theory.

Finally, any bugs in the software should be reduced to breaches of this requirement, or alternatively, bugs in the theory.
"

[[:section {:title "Directly Observable Data"}]]

"*All relevant data structures representing the computational model must be directly observable and measurable at all times. A user must be able to inspect all this data and if required, present it in visual form.*

This requirement ensures that the user of the platform can see what they’re doing at all times. The software is essentially performing a simulation of a simplified version of the neocortex as specified in the CLA, and the user must be able to directly observe how this simulation is progressing and how her choices in configuring the system might affect the computation.

The benefits of this requirement should be reasonably obvious. Two in particular: first, during development, a direct visual confirmation of the results of changes is a powerful tool; and secondly, this answers much of the representation problem, as it allows an observer to directly see how the models in the theory work, rather than relying on analogy.
"

[[:section {:title "Sufficiently Performant"}]]

"*The system must have performance sufficient to provide for rapid development of configurations suitable to a user task. In addition, the performance on large or complex data sets must be sufficient to establish that the system is succeeding in its task in principle, and that simply by scaling or optimising it can perform at “production” levels.* 

What this says is that the system must be a working prototype for how a more finely tuned or higher-performance equivalent will perform. Compute power and memory are cheap, and software can always be made faster relatively easily. The question a user has when using the system is primarily whether or not (and how well) the system can solve her problem, not whether it takes a few seconds or a few hours.

This constraint requires that the software infrastructure be designed so as to allow for significant raw performance improvements, both by algorithm upgrades and also by using concurrency and distribution when the user has the resources to scale the system.
"

[[:section {:title "Useful Metrics"}]]
"*The system must include functionality which allows the user to assess the effectiveness of configuration choices on the system at all relevant levels.* 

At present, NuPIC has some metrics but they are either difficult to understand and interpret, inappropriate, or both. The above requirement must be answered using metrics which have yet to be devised, so we have no further detail at this stage.
"

[[:section {:title "Appropriate Platform"}]]
"
*The development language(s) and runtime platform must ensure ease of deployment, robust execution, easy maintenance and operation, reliability, extensibility, use in new contexts, portability, interoperability, and scaleable performance.*

Quite a list, but each failure in the list reduces the potential mindshare of the software and raises fears for new adopters. Success in every item, along with the other requirements, ensures maximal usefulness and easy uptake by the rising ramp of the adoption curve.
"
(fact 
 (str "Hello World") => "Hello World")

[[:chapter {:title "System Architecture"}]]

"
In order to mirror the HTM/CLA theory, `clortex` has a system architecture which is based on loosely-coupled components communicating over simple channels (or queues).

The primary dataflow in `clortex` involves 'enervation' data structures passing from *sources*, through a possible hierarchy of *regions* and flowing to a set of *sinks*.

Enervation, or inter-region communication, is encoded as a simple **SDR** map, which contains some very basic self-description data (`source`, `bit-size`, `description`, `type` etc) and a list of `on-bits`, `changed-on` and `changed-off` bit indices.

An SDR may also contain a channel which can be used to send the source (or an intermediary) data.
"
(def an-sdr {:source "a-uuid",
             :description "an example SDR",
             :type :scalar-encoding,
             :bit-size 512,
             :topology [512],
             :on-bits #{3, 22, 31, 55, 138},
             :changed-on #{22, 31, 138},
             :changed-off #{6, 111, 220},             
             })



[[:chapter {:title "Data Structures and Functions"}]]

"
The design of `clortex` is based on large, homogenous, passive data structures (e.g. Layers) which are collections of simple structures (e.g. Neurons, Dendrites and Synapses), along with a set of simple functions which act on these data structures (e.g. `(cla/predictive a-neuron)`).
"

[[:section {:title "Neuron"}]]

"*Neurons* in `clortex` are represented as a map as follows:"

(def a-neuron {:active 0,
               :activation-potential 0,
               :feedforward-potential 0,
               :predictive 0,
               :predictive-potential 0,
               :proximal-dendrite [#_[synapse ...]],
               :distal-dendrites [#_[dendrite...]],
               :axon nil
               })

"Neurons with `:active` are designated as **active** neurons. Active neurons represent the layer's SDR, and also will send their signals to downstream neurons for prediction."

"Simple functions act on neurons, such as `predictive?`, defined as follows:"

(defn predictive?
  "checks if a neuron is in the predictive state"
  [neuron]
  (pos? (:predictive neuron)))

(defn set-predictive
  "sets a neuron's predictive state"
  [neuron p]
  (assoc-in neuron [:predictive] p))

"and used like this:"

(facts "neurons have simple predictive states"
 (predictive? a-neuron) => false
 (predictive? (set-predictive a-neuron 1)) => true
 )

[[:section {:title "Synapse"}]]

"Synapses represent connections between neurons."

[[:section {:title "Dendrite"}]]

"A dendrite is a set of synapses. Dendrites can either be *proximal* (meaning *near*) or *distal* (meaning *far*). Each neuron usually has one proximal dendrite, which gathers signals from feedforward sources, and many distal dendrites, which gather predictive signal, usually from nearby active neurons.
"

[[:section {:title "Patch"}]]

"
A Patch is a container for neurons and their connections (synapses). A patch is the system component which links to others and manages incoming and outgoing data, as well as the transformation of the memory in the synapses.
"
