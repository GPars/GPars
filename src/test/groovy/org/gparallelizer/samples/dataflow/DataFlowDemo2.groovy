package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.*

/**
 * Example 2
 * 
 */
List<Integer> ints(int n, int max) {
    if (n == max) return []
    else return [n, * ints(n + 1, max)]
}

List<Integer> sum(int s, List<Integer> stream) {
    switch (stream.size()) {
        case 0: return [s]
        default:
            return [s, * sum(stream[0] + s, stream.size() > 1 ? stream[1..-1] : [])]
    }
}

def x = new DataFlowVariable<List<Integer>>()
def y = new DataFlowVariable<List<Integer>>()

thread { x << ints(0, 1000) }
thread { y << sum(0, x()) }
thread { println("List of sums: " + y()); System.exit(0) }

Thread.sleep 5000