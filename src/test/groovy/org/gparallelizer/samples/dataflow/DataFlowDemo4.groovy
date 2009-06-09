package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.DataFlowStream
import static org.gparallelizer.dataflow.DataFlow.thread

//Example 4

DataFlowActor.DATA_FLOW_GROUP.resize 4

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
    sum(~inStream + s, inStream, outStream)
}

void printSum(DataFlowStream stream) {
    println "Result ${~stream}"
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