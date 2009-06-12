package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import org.gparallelizer.actors.Actor


public class SendAndWaitTest extends GroovyTestCase {

    public void testSuccessfulMessages() {
        CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = PooledActors.actor {
            react {
                reply 2
            }
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        def result = actor.sendAndWait(1)

        latch.await()
        assertEquals 2, result
    }

    public void testMessagesToStoppedActor() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = PooledActors.actor {
            react {
                reply 2
            }
        }

        actor.metaClass.afterStop = {
            barrier.await()
        }

        actor.start()

        def result = actor.sendAndWait(1)
        barrier.await()
        shouldFail(IllegalStateException) {
            actor.sendAndWait 2
        }

        assertEquals 2, result
    }

    public void testFailedMessages() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = PooledActors.actor {
            react {
                reply 2
                barrier.await()
                Thread.sleep 3000  //give the second message time to hit the queue
            }
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        def result = actor.sendAndWait(1)
        barrier.await()
        shouldFail(IllegalStateException) {
            actor.sendAndWait 2
        }

        latch.await()
        assertEquals 2, result
    }

    public void testFailedMessagesOnException() {
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final Actor actor = PooledActors.actor {
            react {
                reply 2
                barrier.await()
                Thread.sleep 3000  //give the second message time to hit the queue
                if (true) throw new RuntimeException('test')
            }
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        def result = actor.sendAndWait(1)
        barrier.await()
        shouldFail(IllegalStateException) {
            actor.sendAndWait 2
        }

        latch.await()
        assertEquals 2, result
    }
}