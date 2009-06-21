package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowStream
import static org.gparallelizer.dataflow.DataFlow.thread

/**
 * A simple producer consumer sample showing use of the DataFlowStream class. 
 */
def words = ['Groovy', 'fantastic', 'concurrency', 'fun', 'enjoy', 'safe', 'GParallelizer', 'data', 'flow']
final def buffer = new DataFlowStream()

thread {
    for (word in words) {
        buffer << word.toUpperCase()
    }
}

thread {
    while(true) println ~buffer
}

System.in.read()