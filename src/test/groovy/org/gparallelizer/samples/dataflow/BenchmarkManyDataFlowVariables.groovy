package org.gparallelizer.samples.dataflow

import static org.gparallelizer.dataflow.DataFlow.*
import org.gparallelizer.dataflow.DataFlowVariable

final many = 1..(6*100*1000)

List dfs = many.collect{ new DataFlowVariable() }
def result = new DataFlowVariable()

def begin = System.nanoTime()

start { result << dfs.sum { it.val } }

dfs.each { df ->
    start { df << 1 }
}

assert many.size() == result.val

def end = System.nanoTime()
println ((end-begin)/1000000)