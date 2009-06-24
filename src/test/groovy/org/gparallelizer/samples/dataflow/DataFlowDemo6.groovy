package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.thread

/**
 * Basic sample showing three green threads cooperating on three variables.
 */

final def x = new DataFlowVariable()
final def y = new DataFlowVariable()
final def z = new DataFlowVariable()

thread {
    z << x.val + y.val
    println "Result: ${z.val}"
}

thread {
    x << 10
}

thread {
    y << 5
}

System.in.read()