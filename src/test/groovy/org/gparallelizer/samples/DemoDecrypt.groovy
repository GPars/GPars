package org.gparallelizer.samples

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import static org.gparallelizer.actors.pooledActors.PooledActors.actor

/**
 * A demo showing two cooperating actors. The decryptor decrypts received messages and replies them back.
 * The console actor sends a message to decrypt, prints out the reply and termitanes both actors.
 * The main thread waits on a latch to prevent premature exit, since both actors use the default pooled actor group,
 * which uses a daemon thread pool.
 * @author Vaclav Pech
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
