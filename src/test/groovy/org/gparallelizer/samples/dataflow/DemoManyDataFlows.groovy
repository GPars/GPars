package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlows
import static org.gparallelizer.dataflow.DataFlow.*

final df = new DataFlows()

final many = 1..(2*100*1000)

// df."1" + df."2" +  ... + df."200000"
start { df.result = many.collect{ df."$it" }.sum() }

// each in a newly started actor:
// df."1" = 1;  df."2" = 1;  ... ;  df."200000" = 1
many.each { num ->
    start { df."$num" = 1 }
}

// Wait for the result to be available
// This is in the main thread, which is DF unaware!
assert many.size() == df.result