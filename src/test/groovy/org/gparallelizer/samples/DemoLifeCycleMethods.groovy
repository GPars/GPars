package org.gparallelizer.samples

import org.gparallelizer.actors.Actors
import org.gparallelizer.actors.DefaultThreadActor

/**
 * Two actors are created to show possible ways to handle all lifecycle events of thread-bound actors.
 * @author Vaclav Pech
 */

final DefaultThreadActor actor1 = Actors.actor {
    println("Running actor1")
    if (true) throw new RuntimeException('test')
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

    onException = {Exception e ->
        println "actor1 threw an exception"
        stop()
    }
}
actor1.start()

Thread.sleep 1000

class LifeCycleSampleActor extends DefaultThreadActor {

    protected void act() {
        println("Running actor2")
        if (true) throw new RuntimeException('test')
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

    private void onException(Exception e) {
        println "actor2 threw an exception"
        stop()
    }
}

new LifeCycleSampleActor().start().join()

