package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start

/**
 * Demonstrates pool resizing. The code would end up deadlocked when the pool didn't resize, since the first two threads
 * wait for each other to bind values to a and b. Only the third thread can unlock the two threads by setting value of a.
 */

DataFlowActor.DATA_FLOW_GROUP.resize 1

final def a = new DataFlowVariable()
final def b = new DataFlowVariable()

start {
    b << 20 + a.val
}

start {
    println "Result: ${b.val}"
    System.exit 0
}

Thread.sleep 2000

start {
    a << 10
}
