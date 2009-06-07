package org.gparallelizer.samples

import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActor
import org.gparallelizer.actors.pooledActors.PooledActors

/**
 * Two actors are created to show possible ways to handle all lifecycle events of event-driven actors.
 * @author Vaclav Pech
 */

private class ExceptionFlag {
    static final boolean THROW_EXCEPTIION = true  //change the flag to test either exception or timeout
}

final PooledActor actor1 = PooledActors.actor {
    println("Running actor1")
    if (ExceptionFlag.THROW_EXCEPTIION) throw new RuntimeException('test')
    else {
        react(10.milliseconds) {}  //will timeout
    }
}
actor1.metaClass {
    afterStart = {->
        println "actor1 has started"
    }

    afterStop = {List undeliveredMessages ->
        println "actor1 has stopped"
    }

    onInterrupt = {InterruptedException e ->
        println "actor2 has been interrupted"
    }

    onTimeout = {->
        println "actor2 has timed out"
    }

    onException = {Exception e ->
        println "actor1 threw an exception"
    }
}
actor1.start()

Thread.sleep 1000

class PooledLifeCycleSampleActor extends AbstractPooledActor {

    protected void act() {
        println("Running actor2")
        if (ExceptionFlag.THROW_EXCEPTIION) throw new RuntimeException('test')
        else {
            react(10.milliseconds) {}  //will timeout
        }
    }

    private void afterStart() {
        println "actor2 has started"
    }

    private void afterStop(List undeliveredMessages) {
        println "actor2 has stopped"
    }

    private void onInterrupt(InterruptedException e) {
        println "actor2 has been interrupted"
    }

    private void onTimeout() {
        println "actor2 has timed out"
    }

    private void onException(Exception e) {
        println "actor2 threw an exception"
    }
}

new PooledLifeCycleSampleActor().start()

System.in.read()