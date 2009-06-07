package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.*

//Example 1
DataFlowVariable<Integer> x = new DataFlowVariable()
DataFlowVariable<Integer> y = new DataFlowVariable()
DataFlowVariable<Integer> z = new DataFlowVariable()
thread {
    z << ~x + ~y
    println "z=${~z}"
    System.exit 0
}

thread {x << 40}
thread {y << 2}

Thread.sleep 5000
