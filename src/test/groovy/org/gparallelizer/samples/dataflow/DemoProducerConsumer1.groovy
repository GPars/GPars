package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowStream
import static org.gparallelizer.dataflow.DataFlow.start

/**
 * A simple producer consumer sample showing use of the DataFlowStream class. 
 */
def words = ['Groovy', 'fantastic', 'concurrency', 'fun', 'enjoy', 'safe', 'GParallelizer', 'data', 'flow']
final def buffer = new DataFlowStream()

start {
    for (word in words) {
        buffer << word.toUpperCase()
    }
}

start {
    while(true) println buffer.val
}

System.in.read()
System.exit 0