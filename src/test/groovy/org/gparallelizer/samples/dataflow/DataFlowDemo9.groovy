package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.thread
import org.gparallelizer.dataflow.StraightDataFlowVariable

final def x = new StraightDataFlowVariable()
final def y = new StraightDataFlowVariable()
final def z = new StraightDataFlowVariable()

thread {
    z << ~x + ~y
    println "Result: ${~z}"
}

thread {
    x << 10
}

thread {
    y << 5
}

System.in.read()