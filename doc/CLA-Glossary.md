### CLA Glossary

:::::::::::::::::::::::::::::::::::META::::::::::::::::::::::::::::::::::

What should go in this area?

This is a re-write of the CLA Whitepaper Glossary (plus any other terms/concepts we want to highlight) where the explanations are targeted at ML/AI researchers.

Terms, concepts and strategies that appear in CLA documentation, presentations, or code should be listed here.

For each term:
* Similar terms or strategies in the traditional community should be briefly discussed.
    * Citations for descriptions of ML/AI terms. e.g. Links to scholarly articles or wikipedia articles.
* The distinction between the CLA term and the traditional term should be carefully noted.
* Links to NuPIC documentation, or code that explore this idea further.

:::::::::::::::::::::::::::::::::::END - META:::::::::::::::::::::::::::

#### <a name="Active_State"></a> Active State
n. A state in which [Cells](#Cell) are active due to [Feed-Forward](#Feed_Forward) input.

* **NN equivalent**: Output of a neuron. Binary valued.

#### <a name="Boosting"></a> Boosting
**TODO**

#### <a name="Bottom_Up"></a> Bottom-Up
adj. Synonym for [Feed-Forward](#Feed_Forward)

#### <a name="Cell"></a> Cells
n. HTM equivalent of a Neuron in NNs. Basic unit of computation in CLA. Cells are more complex than
NN neurons, in that they have two types of [Dendrites](#Dendrite_Segment) -
Proximal ([feed-forward](#Feed_Forward)) and Distal (predictive) - and a more complex internal state.

Instead of having a scalar output as in NN, Cells have a binary [Active State](#Active_State).
Again unlike NNs, Cell activity is based on a combination of factors, not just feed-forward input.

Cells are organized into [Columns](#Column) in HTM regions. In NuPIC, all Cells in a Column share FF input
(ie they have a single proximal dendrite segment for feed-forward).

#### <a name="Coincident_Activity"></a> Coincident Activity
n. Two or more Cells are active at the same time.

#### <a name="Column"></a> Column
n. A group of one or more Cells that function as a unit
in an HTM Region

Cells within a column represent the same feed-forward input, but in different contexts.

#### <a name="Dendrite_Segment"></a> Dendrite Segment
n. A unit of integration of Synapses associated with Cells and Columns

HTMs have two different types of dendrite segments:
* *Distal segments* are associated with lateral connections to a cell, and measure [coincident activity](#Coincident_Activity).  When the number of active synapses on the dendrite segment exceeds a threshold, the associated cell enters the predictive state.
* The *Proximal dendrite* is associated with feed-forward connections to a column (ie shared by the column's cells).  The number of active synapses is summed to generate the feed-forward activation of a column.

#### <a name="Desired_Density"></a> Desired Density [NuPIC-specific]
param. Desired percentage of Columns active due to Feed-Forward input to a Region

The percentage only applies within a radius that varies based on the fan-out of feed-forward inputs.  It is *desired* because the percentage varies some based on the particular input.

The desired density of active columns within a local
 inhibition area (the size of which is set by the
 internally calculated inhibitionRadius, which is in
 turn determined from the average size of the
 connected potential pools of all columns). The
 inhibition logic will ensure that at most N columns
 remain **on** within a local inhibition area, where N =
 localAreaDensity * (total number of columns in
 inhibition area).

#### <a name="Dimensions_Input_SP"></a> Dimensions, Input, Spatial Pooler [NuPIC-specific]
param. A number, list or numpy array representing the dimensions of the input vector.

Format is `[height, width, depth, ...]`, where each value represents the size of the dimension. For a topology of one dimension with 100 inputs use `100`, or `[100]`. For a two dimensional topology of 10x5 use `[10,5]`

#### <a name="Dimensions_Columns_SP"></a> Dimensions, Columns, Spatial Pooler [NuPIC-specific]
param. A number, list or numpy array representing the dimensions of the columns in the region.

Format is `[height, width, depth, ...]`, where each value represents the size of the dimension. For a topology  of one dimesion with 2000 columns use `2000`, or `[2000]`. For a three dimensional topology of 32x64x16 use `[32,64,16]`

#### <a name="Duty_Cycle"></a> Duty Cycle [NuPIC-specific]
param.

**TODO**

#### <a name="Duty_Cycle_Minimum_Percent_Overlap"></a> Duty Cycle, Minimum Percent Overlap [NuPIC-specific]
param. A number between 0 and 1.0, used to set a floor on how often a column
should have at least `stimulusThreshold` active inputs.

**TODO** specify param name `minSomething`

Periodically, each column looks at the overlap duty cycle of
 all other column within its inhibition radius and
 sets its own internal minimal acceptable duty cycle
 to: minPctDutyCycleBeforeInh * max(other columns'
 duty cycles).

 On each iteration, any column whose overlap duty
 cycle falls below this computed value will  get
 all of its permanence values boosted up by
 synPermActiveInc. Raising all permanences in response
 to a sub-par duty cycle before  inhibition allows a
 cell to search for new inputs when either its
 previously learned inputs are no longer ever active,
 or when the vast majority of them have been
 "hijacked" by other columns.

#### Duty Cycle, Minimum Percent Active
**TODO**

A number between 0 and 1.0, used to set a floor on
 how often a column should be activate.
 Periodically, each column looks at the activity duty
 cycle of all other columns within its inhibition
 radius and sets its own internal minimal acceptable
 duty cycle to:
   minPctDutyCycleAfterInh *
   max(other columns' duty cycles).
 On each iteration, any column whose duty cycle after
 inhibition falls below this computed value will get
 its internal boost factor increased.

#### Duty Cycle, Period
**TODO**
The period used to calculate duty cycles. Higher
 values make it take longer to respond to changes in
 boost or synPerConnectedCell. Shorter values make it
 more unstable and likely to oscillate.

#### Encoder
obj. Unlike in NNs, inputs to HTMs are binary vectors (or higher-dimensional matrices). Encoders are needed to
convert real-world or machine-generated data into bit arrays for use by HTM Regions. The output of an encoder
is similar to a [Sparse Distributed Representation](#SDR) (SDR) but usually is not nearly as sparse.

Encoders in NuPIC are often nested to combine different fields in the data (e.g. `timestamp` and `temperature`), and
also to combine different semantic aspects of a single value (e.g. `timestamp.timeofday` and `timestamp.isweekend`).
When creating an encoder, you usually specify the overall width of the output `n` bits, and the width of the window
of `w` on-bits in each encoding.

#### Encoder, Category
obj. Encodes each of a list of categories with a unique set of bits. In NuPIC, a simple category encoder might have the following outputs:

| Input     | Output (`n` = 16, `w` = 4)|
| :-------- |:-------------------------:|
| Animal    | `1111000000000000` |
| Vegetable | `0000111100000000` |
| Mineral   | `0000000011110000` |
| Other     | `0000000000001111` |

#### Encoder, Passthrough
obj. Passes its input straight through to its receiver. What you use when the input is already an [SDR](#SDR),
for example when the output of one Region is passed to a higher-level one.

Also used when the input to the whole system is an SDR, eg. for CEPT Retina representations of text.

#### Encoder, Scalar
obj. Encodes a scalar (floating-point) value.

#### <a name="Feed_Forward"></a> Feed-Forward (aka FF)
Moving in a direction away from an input, or from a lower Level to a higher Level in a Hierarchy (sometimes called Bottom-Up).

In CLA, feed-forward inputs to a [Cell](#Cell) are connected to its

#### Feedback
**TODO** moving in a direction towards an input, or from a higher Level to a lower level in a Hierarchy (sometimes called Top-Down)

#### First Order Prediction
**TODO** a prediction based only on the current input and not on the prior inputs – compare to Variable Order Prediction

#### Hierarchical Temporal Memory (HTM)
**TODO** a technology that replicates some of the structural and
algorithmic functions of the neocortex

#### Hierarchy
**TODO** a network of connected elements where the connections between the elements are uniquely identified as Feed-Forward or Feedback

#### HTM Cortical Learning Algorithms
**TODO** the suite of functions for Spatial Pooling, Temporal
Pooling, and learning and forgetting that comprise an HTM Region, also referred to as HTM Learning Algorithms

#### HTM Network
**TODO** a Hierarchy of HTM Regions

#### HTM Region
**TODO** the main unit of memory and Prediction in an HTM

An HTM region is comprised of a layer of highly interconnected cells arranged in columns.  An HTM region today has a single layer of cells, whereas in the neocortex (and ultimately in HTM), a region will have multiple layers of cells.  When referred to in the context of it’s position in a hierarchy, a region may be referred to as a level.

#### Inference
**TODO** recognizing a spatial and temporal input pattern as similar to previously learned patterns

#### Inhibition
**TODO**

#### Inhibition, Number Active Per Area - Spatial Pooler
**TODO**

An alternate way to control the density of the active
 columns. If numActivePerInhArea is specified then
 localAreaDensity must less than 0, and vice versa.
 When using numActivePerInhArea, the inhibition logic
 will insure that at most 'numActivePerInhArea'
 columns remain ON within a local inhibition area (the
 size of which is set by the internally calculated
 inhibitionRadius, which is in turn determined from
 the average size of the connected receptive fields of
 all columns). When using this method, as columns
 learn and grow their effective receptive fields, the
 inhibitionRadius will grow, and hence the net density
 of the active columns will *decrease*. This is in
 contrast to the localAreaDensity method, which keeps
 the density of active columns the same regardless of
 the size of their receptive fields.

#### Inhibition, Global - Spatial Pooler
**TODO** defines the area around a Column that it actively inhibits

If true, then during inhibition phase the winning
 columns are selected as the most active columns from
 the region as a whole. Otherwise, the winning columns
 are selected with resepct to their local
 neighborhoods. using global inhibition boosts
 performance x60.

#### Inhibition Radius
**TODO** defines the area around a Column that it actively inhibits

#### Input, Encoder
**TODO**

#### Input - Spatial Pooler
**TODO**

#### Input - Temporal Pooler
**TODO**

#### Lateral Connections
**TODO** connections between Cells within the same Region

#### Level
**TODO** an HTM Region in the context of the Hierarchy

#### Neuron
**TODO** an information processing Cell in the brain

In this document, we use the word neuron specifically when referring to biological cells, and “cell” when referring to the HTM unit of computation.

#### Overlap - Spatial Pooler
**TODO**

#### Permanence
**TODO** a scalar value which indicates the connection state of a Potential Synapse

A permanence value below a threshold indicates the synapse is not formed.  A permanence value above the threshold indicates the synapse is valid.  Learning in an HTM region is accomplished by modifying permanence values of potential synapses.

#### Permanence Decrement
**TODO** The amount by which an inactive synapse is decremented in each round.

#### Permanence Decrement, Orphaned columns
**TODO** The amount by which an inactive synapse is decremented in each round.

The amount by which to decrease the permanence of an
 active synapse on a column which has high overlap
 with the input, but was inhibited (an "orphan"
 column).

#### Permanence Increment
**TODO**
The amount by which an active synapse is incremented
 in each round.

#### Potential Synapse (Pool)
**TODO** the subset of all Cells that could potentially form Synapses with a particular Dendrite Segment

Only a subset of potential synapses will be valid synapses at any time based on their permanence value.

#### Potential Radius - Spatial Pooler
**TODO** the subset of all Cells that could potentially form Synapses with a particular Dendrite Segment

This parameter determines the extent of the input
 that each column can potentially be connected to.
 This can be thought of as the input bits that
 are visible to each column, or a 'receptiveField' of
 the field of vision. A large enough value will result
 in the 'global coverage', meaning that each column
 can potentially be connected to every input bit. This
 parameter defines a square (or hyper square) area: a
 column will have a max square potential pool with
 sides of length 2 * potentialRadius + 1.

#### Potential Percent - Spatial Pooler

The percent of the inputs, within a column's
 potential radius, that a column can be connected to.
 If set to 1, the column will be connected to every
 input within its potential radius. This parameter is
 used to give each column a unique potential pool when
 a large potentialRadius causes overlap between the
 columns. At initialization time we choose
 ((2*potentialRadius + 1)^(# inputDimensions) *
 potentialPct) input bits to comprise the column's
 potential pool.

#### Prediction
**TODO** activating Cells (into a predictive state) that will likely become active in the near future due to Feed-Forward input

An HTM region often predicts many possible future inputs at the same time.

#### Receptive Field
**TODO** the set of inputs to which a Column or Cell is connected

If the input to an HTM region is organized as a 2D array of bits, then the receptive field can be expressed as a radius within the input space.

#### Sensor
**TODO** a source of inputs for an HTM Network

#### Sparse Distributed Representation (SDR)
**TODO** representation comprised of many bits in which a small percentage are active and where no single bit is sufficient to convey meaning

Kanerva - http://en.wikipedia.org/wiki/Sparse_distributed_memory

#### Spatial Pooler
**TODO**

#### Spatial Pooling
**TODO** the process of forming a sparse distributed representation of an input

One of the properties of spatial pooling is that overlapping input patterns map to the same sparse distributed representation.

#### Sub-Sampling
**TODO** recognizing a large distributed pattern by matching only a small subset of the active bits in the large pattern

#### Synapse
**TODO** connection between Cells formed while learning

#### Temporal Pooler
**TODO**

#### Temporal Pooling
**TODO** the process of forming a representation of a sequence of input patterns where the resulting representation is more stable than the input

#### Threshold, Connected - Spatial Pooler
**TODO**

The default connected threshold. Any synapse whose
 permanence value is above the connected threshold is
 a "connected synapse", meaning it can contribute to
 the cell's firing.

#### Threshold, Stimulus - Spatial Pooler
**TODO**

This is a number specifying the minimum number of
 synapses that must be on in order for a columns to
 turn ON. The purpose of this is to prevent noise
 input from activating columns.


#### Top-Down
**TODO** synonym for Feedback

#### Variable Order Prediction
**TODO** a prediction based on varying amounts of prior context – compare to First Order Prediction

It is called “variable” because the memory to maintain prior context is allocated as needed.  Thus a memory system that implements variable order prediction can use context going way back in time without requiring exponential amounts of memory.
