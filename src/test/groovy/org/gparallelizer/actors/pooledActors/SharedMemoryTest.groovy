package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.Actor
import static org.gparallelizer.actors.pooledActors.PooledActors.actor
import static org.gparallelizer.actors.pooledActors.PooledActors.getPool

public class SharedMemoryTest extends GroovyTestCase {

    public void testSharedAccess() {
        long counter = 0

        getPool().resize 2
        def latch = new CountDownLatch(1)

        Actor actor1 = actor {
            loop {
                react {
                    assert it == counter * 2
                    counter += 1
                    it.reply counter.longValue() * 2
                }
            }
        }.start()

        Actor actor2 = actor {
            loop {
                if (counter < 10000) actor1.send counter.longValue() * 2
                else {
                    actor1.stop()
                    stop()
                    latch.countDown()
                }
                react {
                    assert it == counter * 2
                    counter += 1
                }
            }
        }.start()


        latch.await()
        assertEquals 10000, counter
        getPool().shutdown()
    }
}