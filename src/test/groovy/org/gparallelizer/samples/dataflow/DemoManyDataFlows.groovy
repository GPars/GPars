package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlows
import static org.gparallelizer.dataflow.DataFlow.*

static final LIMIT = 3*100*1000

final df = new DataFlows(LIMIT+1, 0.9f, DataFlows.MAX_SEGMENTS)

final many = 1..LIMIT

start { df.result = many.collect{ df[it] }.sum() }

// each in a newly started actor:
many.each { num ->
    start { df[num] = 1 }
}

// Wait for the result to be available
// This is in the main thread, which is DF unaware!
assert many.size() == df.result