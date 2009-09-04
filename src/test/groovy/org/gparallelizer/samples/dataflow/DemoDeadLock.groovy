package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start

/**
 * Demonstrates that deadlocks are deterministic in dataflow concurrency model. The deadlock appears reliably every time
 * the sample is run.
 * Also shows a way to exchange messages among threads and set and handle timeouts.
 */

final def a = new DataFlowVariable()
final def b = new DataFlowVariable()

final def actor = start {

    delegate.metaClass.onTimeout = {
        println 'Deadlock detected'
        System.exit 0
    }

    react(5.seconds) {x, y ->
        println "Got replies: a:${x} b:${b}"
    }
}

start {
    b << 20 + a.val
    actor.send b.val
}

start {
    a << 10 + b.val
    actor.send a.val
}

