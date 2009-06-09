package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.DataFlowStream
import static org.gparallelizer.dataflow.DataFlow.thread

//Example 3

DataFlowActor.DATA_FLOW_GROUP.resize 4

void ints(int n, int max, DataFlowStream<Integer> stream) {
    if (n != max) {
        println "Generating int: $n"
        stream << n
        ints(n+1, max, stream)
    }
}

final def producer = new DataFlowStream<Integer>()

thread {
    ints(0, 1000, producer)
}

thread {
    Thread.sleep(1000)
    println "Sum: ${producer.collect{it * it}.inject(0){sum, x -> sum + x}}"
    System.exit 0
}

System.in.read()