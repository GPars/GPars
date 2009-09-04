package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlows
import static org.gparallelizer.dataflow.DataFlow.*

final df = new DataFlows()

start { df.x = df.y }

// Example of how to introduce non-determinism into df usage.
// Don't do it!
start { if (Math.random() < 0.5f) df.y = 1  else df.y = df.x }

assert 1 == df.x