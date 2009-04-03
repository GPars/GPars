import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import static org.gparallelizer.actors.pooledActors.PooledActors.actor
import static org.gparallelizer.actors.pooledActors.PooledActors.getPool

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Mar 2, 2009
 */
final CountDownLatch latch = new CountDownLatch(2)

final def decryptor = actor {
    loop {
        react {String message->
            if ('stopService' == message) stop()
            else reply message.reverse()
        }
    }
}.start()

decryptor.metaClass {
    afterStop = {
        println 'Stopped'
        latch.countDown()
    }
}

final AbstractPooledActor console = actor {
    decryptor.send 'suonorhcnysa si yvoorG'
    react {
        println 'Decrypted message: ' + it
        decryptor.send 'stopService'
    }
}
console.start()

console.metaClass {
    afterStop = {
        println 'Stopped'
        latch.countDown()
    }
}

latch.await()
println 'Shutdown'
getPool().shutdown()
