package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowStream
import static org.gparallelizer.dataflow.DataFlow.thread

/**
 * A producer consumer sample, where the producer generates numbers into the DataFlowStream and the consumer
 * takes a snapshot of the DataFlowStream using the collect() method to calculate summary of the numbers in the stream.
 */

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
