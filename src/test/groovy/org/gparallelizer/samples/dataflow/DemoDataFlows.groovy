package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlows
import static org.gparallelizer.dataflow.DataFlow.*

final df = new DataFlows()

thread {
    df.z = df.x + df.y
    println "Result: ${df.z}"
    System.exit 0
}

thread {
    df.x = 10
}

thread {
    df.y = 5
}