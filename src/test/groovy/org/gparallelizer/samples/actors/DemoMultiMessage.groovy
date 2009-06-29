package org.gparallelizer.samples

import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActorGroup
import static java.util.concurrent.TimeUnit.*

/**
 * Demonstrates the ability to receive multiple messages and selectively reply to some of them.
 * Notice the ability to sent HashMaps as well as instances of the Offer class as messages.
 * A custom pooled actor group is used to group the actors with a single thread pool.
 * The actors, which have submitted their offers, terminate, if they don't hear back from the actor within a timeout.
 * The main threads joins all actors to wait for their termination, since we're using a non-deamon actor group.
 * @author Vaclav Pech
 */

final PooledActorGroup group = new PooledActorGroup(1)

final AbstractPooledActor actor = group.actor {
    react {offerA, offerB, offerC ->
        reply 'Received your kind offer. Now processing it and comparing with others.'  //sent to all senders
        def winnerOffer = [offerA, offerB, offerC].min {it.price}
        winnerOffer.reply 'I accept your reasonable offer'  //sent to the winner only
        ([offerA, offerB, offerC] - [winnerOffer])*.reply 'Maybe next time'  //sent to the losers only
    }
}
actor.start()

final def a1 = group.actor {
    actor << new Offer(price: 10)
    loop {
        react(3, SECONDS) {
            println "Agent 1: $it"
        }
    }
}
a1.start()

final def a2 = group.actor {
    actor << [price:20]
    loop {
        react(3, SECONDS) {
            println "Agent 2: $it"
        }
    }
}.start()

final def a3 = group.actor {
    actor << new Offer(price : 5)
    loop {
        react(3, SECONDS) {
            println "Agent 3: $it"
        }
    }
}.start()

[actor, a1, a2, a3]*.join()

class Offer {
    int price
}


