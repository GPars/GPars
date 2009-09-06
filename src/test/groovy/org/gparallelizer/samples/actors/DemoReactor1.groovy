package org.gparallelizer.samples.actors

import org.gparallelizer.actors.pooledActors.PooledActorGroup

/**
 * Demonstrates use of reactor - a specialized actor responding to incomming messages with result of running its body
 * on the message.
 */

final def group = new PooledActorGroup()

final def doubler = group.reactor {
    2 * it
}.start()

group.actor {
    println 'Double of 10 = ' + doubler.sendAndWait(10)
}.start()

group.actor {
    println 'Double of 20 = ' + doubler.sendAndWait(20)
}.start()

group.actor {
    println 'Double of 30 = ' + doubler.sendAndWait(30)
}.start()

for(i in (1..10)) {
    println "Double of $i = ${doubler.sendAndWait(i)}"
}

doubler.stop()
doubler.join()
