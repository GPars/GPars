package org.gparallelizer.dataflow

import static org.gparallelizer.dataflow.DataFlow.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

public class ThreadLifeCycleTest extends GroovyTestCase {

    public void testActorGroup() {
        final AbstractPooledActor actor = thread {
            react {}
        }
        assertEquals DataFlowActor.DATA_FLOW_GROUP, actor.actorGroup
        actor << 'Message'
    }

    public void testBasicLifeCycle() {
        AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        final def thread = thread {
            enhance(delegate, counter, latch)
            counter.incrementAndGet()
        }
        latch.await()
        assertEquals 2, counter.get()
    }

    public void testExceptionLifeCycle() {
        AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        final def thread = thread {
            enhance(delegate, counter, latch)
            counter.incrementAndGet()
            if (true) throw new RuntimeException('test')
        }
        latch.await()
        assertEquals 3, counter.get()
    }
    public void testTimeoutLifeCycle() {
        AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        final def thread = thread {
            enhance(delegate, counter, latch)
            counter.incrementAndGet()
                react(10.milliseconds) {}  //will timeout
        }
        latch.await()
        assertEquals 3, counter.get()
    }

    private void enhance(final AbstractPooledActor thread, final AtomicInteger counter, final CountDownLatch latch) {

        thread.metaClass {
            afterStart = {->  //won't be called
                counter.incrementAndGet()
            }

            afterStop = {List undeliveredMessages ->
                counter.incrementAndGet()
                latch.countDown()
            }

            onInterrupt = {InterruptedException e ->
                counter.incrementAndGet()
            }

            onTimeout = {->
                counter.incrementAndGet()
            }

            onException = {Exception e ->
                counter.incrementAndGet()
            }
        }
    }

}