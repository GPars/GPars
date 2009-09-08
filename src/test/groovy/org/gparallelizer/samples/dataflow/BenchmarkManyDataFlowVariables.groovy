package org.gparallelizer.samples.dataflow

import static org.gparallelizer.dataflow.DataFlow.*
import org.gparallelizer.dataflow.DataFlowVariable
import java.util.concurrent.Executors

final many = 1..(limit)

List dfs = many.collect{ new DataFlowVariable() }
def result = new DataFlowVariable()

def scheduler = Executors.newFixedThreadPool (20)

scheduler.execute { result << dfs.sum { it.val } }

dfs.each { df ->
    scheduler.execute { df << 1 }
}

assert many.size() == result.val

scheduler.shutdown ()