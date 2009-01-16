package org.gparallelizer.actors

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 *
 * @author Vaclav Pech
 * Date: Jan 16, 2009
 */

public class TimeCategoryActorsTest extends GroovyTestCase {
    public void testReceive() {
        volatile def result=''
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.oneShotActor {
            result = receive(3.seconds)
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assertNull(result)
    }

    public void testReceiveWithHandler() {
        volatile def result=''
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.oneShotActor {
            receive(2.seconds) {
                result = it
            }
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assertNull(result)
    }
}