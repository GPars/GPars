package org.gparallelizer.samples.dataflow

import static org.gparallelizer.dataflow.DataFlow.*
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

def throwException = true

final def thread = thread {
    enhance(delegate)
    println("Running thread")
    if (throwException) throw new RuntimeException('test')
    else {
        react(10.milliseconds) {}  //will timeout
    }
}

private void enhance(AbstractPooledActor thread) {
    thread.metaClass {
        afterStop = {List undeliveredMessages ->
            println "thread has stopped"
        }

        onInterrupt = {InterruptedException e ->
            println "thread has been interrupted"
        }

        onTimeout = {->
            println "thread has timed out"
        }

        onException = {Exception e ->
            println "thread threw an exception"
        }
    }
}

System.in.read()