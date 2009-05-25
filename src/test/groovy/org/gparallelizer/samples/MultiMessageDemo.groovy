import org.gparallelizer.actors.Actors
import org.gparallelizer.actors.DefaultActor
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

final AbstractPooledActor actor = PooledActors.actor {
    react {offerA, offerB, offerC ->
        reply 'Received your kind offer. Now processing it and comparing with others.'  //sent to all senders
        def winnerOffer = [offerA, offerB, offerC].min {it.price}
        winnerOffer.reply 'I accept your reasonable offer'  //sent to the winner only
    }
}
actor.start()

PooledActors.actor {
    actor << new Offer(price : 10)
    loop {
        react {
            println "Agent 1: $it"
        }
    }
}.start()

PooledActors.actor {
    actor << [price:20]
    loop {
        react {
            println "Agent 2: $it"
        }
    }
}.start()

PooledActors.actor {
    actor << new Offer(price : 5)
    loop {
        react {
            println "Agent 3: $it"
        }
    }
}.start()

Thread.sleep 1000

class Offer {
    int price
}

assert 1.metaClass
assert 'value'.metaClass
assert [].metaClass
assert [:].metaClass == null
assert [metaClass:'ksdhfjksd'].metaClass
assert [:].getMetaClass()
