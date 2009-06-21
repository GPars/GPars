package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.thread
import org.gparallelizer.dataflow.DataFlowVariable

/**
 * Shows threads manipulating mutually dependant 4 variables.
 */

DataFlowVariable<Integer> x = new DataFlowVariable<Integer>()
DataFlowVariable<Integer> y = new DataFlowVariable<Integer>()
DataFlowVariable<Integer> z = new DataFlowVariable<Integer>()
DataFlowVariable<Integer> v = new DataFlowVariable<Integer>()

thread {
    println 'Thread main'

    x << 1

    println("'x' set to: " + ~x)
    println("Waiting for 'y' to be set...")

    if (~x > ~y) {
        z << x
        println("'z' set to 'x': " + ~z)
    } else {
        z << y
        println("'z' set to 'y': " + ~z)
    }
 }

thread {
    println("Thread 'setY', sleeping...")
    Thread.sleep(5000)
    y << 2
    println("'y' set to: " + ~y)
}

thread {
    println("Thread 'setV'")
    v << y
    println("'v' set to 'y': " + ~v)
}

System.in.read()