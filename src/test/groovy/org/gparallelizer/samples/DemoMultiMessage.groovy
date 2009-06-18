package org.gparallelizer.samples

import org.gparallelizer.actors.Actors
import org.gparallelizer.actors.DefaultThreadActor
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActorGroup

/**
 * Demonstrates the ability to receive multiple messages and selectively reply to some of them.
 * Notice the ability to sent HashMaps as well as instances of the Offer class as messages.
 * A custom pooled actor group is used to group the actors with a single thread pool.
 * @author Vaclav Pech
 */

final PooledActorGroup group = new PooledActorGroup(1)

final AbstractPooledActor actor = group.actor {
    react {offerA, offerB, offerC ->
        reply 'Received your kind offer. Now processing it and comparing with others.'  //sent to all senders
        def winnerOffer = [offerA, offerB, offerC].min {it.price}
        winnerOffer.reply 'I accept your reasonable offer'  //sent to the winner only
    }
}
actor.start()

group.actor {
    actor << new Offer(price : 10)
    loop {
        react {
            println "Agent 1: $it"
        }
    }
}.start()

group.actor {
    actor << [price:20]
    loop {
        react {
            println "Agent 2: $it"
        }
    }
}.start()

group.actor {
    actor << new Offer(price : 5)
    loop {
        react {
            println "Agent 3: $it"
        }
    }
}.start()

System.in.read()

class Offer {
    int price
}

assert 1.metaClass
assert 'value'.metaClass
assert [].metaClass
assert [:].metaClass == null
assert [metaClass:'ksdhfjksd'].metaClass
assert [:].getMetaClass()
