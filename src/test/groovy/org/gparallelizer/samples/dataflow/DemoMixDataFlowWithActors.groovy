package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.thread
import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

final DataFlowVariable a = new DataFlowVariable()

final AbstractPooledActor doubler = PooledActors.actor {
    react {
        a << 2 * it
    }
}.start()

final AbstractPooledActor thread = thread {
    react {
        doubler << it  //send a number to the doubler
        println "Result ${~a}"  //wait for the result to be bound to 'a'
    }
}

thread << 10

System.in.read()