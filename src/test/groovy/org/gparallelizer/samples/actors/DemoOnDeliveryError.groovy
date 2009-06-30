import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.CyclicBarrier

/**
 * Shows possibilities to handle message delivery errors
 */

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
        println 'AAAAAAAAAAAAAAAAa'
//        delegate.reply "Could not deliver #delegate"
        me << "Could not deliver #delegate"
    }

    message3.metaClass.onDeliveryError = {->
        println 'BBBBBBBBBBBBBBBBBBBBBBBBBBBB'
        me << "Could not deliver #delegate"
    }

    actor << message1
    actor << message2
    actor << message3
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