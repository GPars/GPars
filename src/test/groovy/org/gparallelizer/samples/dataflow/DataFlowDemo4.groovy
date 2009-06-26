package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowStream
import static org.gparallelizer.dataflow.DataFlow.thread

/**
 * A producer consumer sample, where the producer generates numbers into the DataFlowStream, the intermediate consumer
 * keeps reading numbers from the producer, calculates the summary of numberf it saw so far and passes the summaries
 * on to the final consumer, which prints them out.
 * Since both consumers read elements using the 'val' property, they will keep reading until stopped explicitely.
 */

void ints(int n, int max, DataFlowStream<Integer> stream) {
    if (n != max) {
        println "Generating int: $n"
        stream << n
        ints(n+1, max, stream)
    }
}

void sum(int s, DataFlowStream<Integer> inStream, DataFlowStream<Integer> outStream) {
    println "Calculating $s"
    outStream << s
    sum(inStream.val + s, inStream, outStream)
}

void printSum(DataFlowStream stream) {
    println "Result ${stream.val}"
    printSum stream
}

final def producer = new DataFlowStream<Integer>()
final def consumer = new DataFlowStream<Integer>()

thread {
    ints(0, 1000, producer)
}

thread {
    sum(0, producer, consumer)
}

thread {
    printSum (consumer)
}

System.in.read()
System.exit 0