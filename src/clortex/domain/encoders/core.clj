(ns clortex.domain.encoders.core
"
## [Pre-alpha] Standard Encoders

The Cortical Learning Algorithm consumes data encoded as **Sparse Distributed
Representations** (SDRs), which are arrays or matrices of binary digits (bits).
The functions which convert values into SDRs are `clortex` **encoders**.

**TODO**: Factor out encoder functions. Use Graph or protocols?
"
  (:require [clortex.protocols :refer :all]
            [clortex.utils.hash :refer [sha1 mod-2]]
            [clortex.domain.encoders.scalar :refer :all]))

