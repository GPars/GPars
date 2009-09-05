package org.gparallelizer.samples.dataflow

import static org.gparallelizer.dataflow.DataFlow.*
import org.gparallelizer.dataflow.DataFlowVariable 

final many = 1..(limit)

List dfs = many.collect{ new DataFlowVariable() }
def result = new DataFlowVariable()

start { result << dfs.sum { it.val } }

dfs.each { df ->
    start { df << 1 }
}

assert many.size() == result.val