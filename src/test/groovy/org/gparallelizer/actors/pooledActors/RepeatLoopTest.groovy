package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch

public class RepeatLoopTest extends GroovyTestCase {

    public void testLoopWihtoutReact() {
        volatile int count = 0
        final CountDownLatch latch = new CountDownLatch(1)

        PooledActors.actor {
            loop {
                if (count == 10) {
                    stop()
                    latch.countDown()
                    return
                }
                count+=1
            }
        }.start()

        latch.await()
        assertEquals 10, count
    }
}