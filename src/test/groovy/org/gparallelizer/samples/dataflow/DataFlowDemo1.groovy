package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable as WAIT
import static org.gparallelizer.dataflow.DataFlow.*

/**
 * Basic sample showing three green threads cooperating on three variables.
 */
WAIT<Integer> x = new WAIT()
WAIT<Integer> y = new WAIT()
WAIT<Integer> z = new WAIT()

start { z << x.val + y.val }

start { x << 40 }
start { y << 2 }

println "z=${z.val}"
assert 42 == z.val