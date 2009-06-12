package org.gparallelizer.actors

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author Vaclav Pech
 * Date: Jan 9, 2009
 */
public class ActorsTest extends GroovyTestCase {
    public void testDefaultActor() {
        final AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.defaultActor {
            final int value = counter.incrementAndGet()
            if (value==3) {
                stop()
                latch.countDown()
            }
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 3, counter.intValue()
    }

    public void testDefaultOneShotActor() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.defaultOneShotActor {
            flag.set(true)
            receive(10, TimeUnit.MILLISECONDS)
        }
        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assert flag.get()
    }

    public void testDefaultOneShotActorWithException() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.defaultOneShotActor {
            throw new RuntimeException('test')
        }
        actor.metaClass.onException = {}
        actor.metaClass.afterStop = {
            flag.set(true)
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assert flag.get()
    }

    public void testSynchronousActor() {
        final AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.synchronousActor {
            final int value = counter.incrementAndGet()
            if (value==3) {
                stop()
                latch.countDown()
            }
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 3, counter.intValue()
    }

    public void testSynchronousOneShotActor() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.synchronousOneShotActor {
            flag.set(true)
            receive(10, TimeUnit.MILLISECONDS)
        }
        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assert flag.get()
    }

    public void testSynchronousOneShotActorWithException() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.synchronousOneShotActor {
            if (true) throw new RuntimeException('test')
        }
        actor.metaClass.onException = {}
        actor.metaClass.afterStop = {
            flag.set(true)
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assert flag.get()
    }


    public void testBoundedActor() {
        final AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.boundedActor {
            final int value = counter.incrementAndGet()
            if (value==3) {
                stop()
                latch.countDown()
            }
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 3, counter.intValue()
    }

    public void testBoundedOneShotActor() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.boundedOneShotActor {
            flag.set(true)
            receive(10, TimeUnit.MILLISECONDS)
        }
        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assert flag.get()
    }

    public void testBoundedOneShotActorWithException() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.boundedOneShotActor {
            if (true) throw new RuntimeException('test')
        }
        actor.metaClass.onException = {}
        actor.metaClass.afterStop = {
            flag.set(true)
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assert flag.get()
    }

    public void testBoundedActorWithCustomCapacity() {
        final AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.boundedActor(5) {
            final int value = counter.incrementAndGet()
            if (value==3) {
                stop()
                latch.countDown()
            }
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 3, counter.intValue()
    }

    public void testBoundedOneShotActorWithCustomCapacity() {
        final AtomicBoolean flag = new AtomicBoolean(false)
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.boundedOneShotActor(5) {
            flag.set(true)
            receive(10, TimeUnit.MILLISECONDS)
        }
        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assert flag.get()
    }

}