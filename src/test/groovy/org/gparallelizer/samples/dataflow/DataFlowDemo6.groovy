package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start

/**
 * Basic sample showing three green threads cooperating on three variables.
 */

final def x = new DataFlowVariable()
final def y = new DataFlowVariable()
final def z = new DataFlowVariable()

start {
    z << x.val + y.val
    println "Result: ${z.val}"
    System.exit 0
}

start {
    x << 10
}

start {
    y << 5
}
