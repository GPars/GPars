package org.gparallelizer.samples.actors

import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.PooledActors

/**
 * Demonstrates use of reactor - a specialized actor responding to incomming messages with result of running its body
 * on the message.
 */

final def doubler = PooledActors.reactor {
    2 * it
}.start()

Actor actor = PooledActors.actor {
    (1..10).each {doubler << it}
    int i = 0
    loop {
        i += 1
        if (i > 10) stop()
        else {
            react {message ->
                println "Double of $i = $message"
            }
        }
    }
}.start()

actor.join()
doubler.stop()
doubler.join()
