package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 *
 * @author Vaclav Pech
 * Date: Jan 12, 2009
 */
public class LifecycleTest extends GroovyTestCase {
    public void testOnInterrupt() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultActor actor = Actors.actor {throw new InterruptedException('test')}
        actor.metaClass.onInterrupt = {
            exception=it
            latch.countDown()
        }

        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof InterruptedException
    }

    public void testOnException() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultActor actor = Actors.actor {throw new RuntimeException('test')}
        actor.metaClass.onException = {
            exception=it
            latch.countDown()
        }

        actor.metaClass.afterStop = {
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof RuntimeException
        assertEquals('test', exception.message)
        assert actor.isActive()
    }

    public void testOneShotOnInterrupt() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultActor actor = Actors.oneShotActor {throw new InterruptedException('test')}
        actor.metaClass.onInterrupt = {
            exception=it
            latch.countDown()
        }

        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof InterruptedException
    }

    public void testOnShotOnException() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultActor actor = Actors.oneShotActor {throw new RuntimeException('test')}
        actor.metaClass.onException = {
            exception=it
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof RuntimeException
        assertEquals('test', exception.message)
        assert !actor.isActive()
    }

    public void testOnExceptionHandlerCanStopTheActor() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultActor actor = Actors.actor {throw new RuntimeException('test')}
        actor.metaClass.onException = {
            exception=it
            stop()
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof RuntimeException
        assertEquals('test', exception.message)
        assert !actor.isActive()
    }

    public void testExceptionInOnExceptionHandlerStopsTheActor() {
        volatile Object exception = null
        final CountDownLatch latch = new CountDownLatch(1)

        final DefaultActor actor = Actors.actor {throw new RuntimeException('test')}
        actor.metaClass.onException = {
            exception=it
            throw new RuntimeException('testing failed handler')
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)

        assert exception instanceof RuntimeException
        assertEquals('test', exception.message)
        assert !actor.isActive()
    }
}