import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.CyclicBarrier

/**
 * Shows possibilities to handle message delivery errors.
 * When an actor terminates, unprocessed messages from its queue have their onDeliveryError() method called.
 * The onDeliveryError() method can, for example, send a notification back to the original sender of the message.
 */

PooledActors.defaultPooledActorGroup.resize 10
final CyclicBarrier barrier = new CyclicBarrier(2)

final AbstractPooledActor actor = PooledActors.actor {
    barrier.await()
    react {
        stop()
    }
}.start()

final AbstractPooledActor me
me = PooledActors.actor {
    def message1 = 1
    def message2 = 2
    def message3 = 3

    message2.metaClass.onDeliveryError = {->
        me << "Could not deliver $delegate"
    }

    message3.metaClass.onDeliveryError = {->
        me << "Could not deliver $delegate"
    }

    actor << message1
    actor << message2
    actor << message3
    Thread.sleep 1000 
    barrier.await()

    react {a->
        println a
        react {b ->
            println b
            System.exit 0 
        }
    }

}.start()

System.in.read()