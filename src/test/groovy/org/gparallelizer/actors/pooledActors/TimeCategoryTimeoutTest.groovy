package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicBoolean
import static org.gparallelizer.actors.pooledActors.PooledActors.actor
import java.util.concurrent.atomic.AtomicInteger


/**
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public class TimeCategoryTimeoutTest extends GroovyTestCase {
    protected void setUp() {
        super.setUp();
        PooledActors.defaultPooledActorGroup.resize(5)
    }

    public void testTimeout() {
        final def barrier = new CyclicBarrier(2)
        final AtomicBoolean codeFlag = new AtomicBoolean(false)
        final AtomicBoolean timeoutFlag = new AtomicBoolean(false)

        final PooledActor actor = actor {
            loop {
                react(1.second) {
                    codeFlag.set(true)  //should never reach
                }
            }
        }.start()

        actor.metaClass {
            onTimeout = {-> timeoutFlag.set(true) }
            afterStop = {messages -> barrier.await() }
        }

        barrier.await()
        assertFalse codeFlag.get()
        assert timeoutFlag.get()
    }

    public void testMessageBeforeTimeout() {
        final def barrier = new CyclicBarrier(2)
        final AtomicBoolean codeFlag = new AtomicBoolean(false)
        final AtomicBoolean nestedCodeFlag = new AtomicBoolean(false)
        final AtomicBoolean timeoutFlag = new AtomicBoolean(false)

        final PooledActor actor = actor {
            loop {
                barrier.await()
                react(5000.milliseconds) {
                    codeFlag.set(true)
                    react(1.second) {
                        nestedCodeFlag.set(true)  //should never reach
                    }
                }
            }
        }.start()

        actor.metaClass {
            onTimeout = {-> timeoutFlag.set(true) }
            afterStop = {messages -> barrier.await() }
        }

        barrier.await()
        actor.send 'message'

        barrier.await()
        assert codeFlag.get()
        assertFalse nestedCodeFlag.get()
        assert timeoutFlag.get()
    }

    public void testTimeoutInLoop() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger codeCounter = new AtomicInteger(0)
        final AtomicBoolean timeoutFlag = new AtomicBoolean(false)

        final PooledActor actor = actor {
            loop {
                barrier.await()
                react(1.second) {
                    codeCounter.incrementAndGet()
                }
            }
        }.start()

        actor.metaClass {
            onTimeout = {-> timeoutFlag.set(true) }
            afterStop = {messages -> barrier.await() }
        }

        barrier.await()
        actor.send 'message'
        barrier.await()

        barrier.await()
        assertEquals(1, codeCounter.get())
        assert timeoutFlag.get()
    }
}