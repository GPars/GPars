package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.pooledActors.PooledActors
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import static org.gparallelizer.actors.pooledActors.PooledActors.*

/**
 *
 * @author Vaclav Pech
 * Date: Feb 20, 2009
 */
public class MessagingTest extends GroovyTestCase {
    protected void setUp() {
        super.setUp();
        PooledActors.pool.resize(10)
    }

    public void testReact() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final AbstractPooledActor actor = actor {
            counter.incrementAndGet()
            barrier.await()
            react {
                counter.incrementAndGet()
                barrier.await()
                react {
                    counter.incrementAndGet()
                    barrier.await()
                }
            }
        }.start()

        barrier.await()
        assertEquals 1, counter.intValue()

        actor.send('message')
        barrier.await()
        assertEquals 2, counter.intValue()

        actor.send('message')
        barrier.await()
        assertEquals 3, counter.intValue()
    }

    public void testLeftShift() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final AbstractPooledActor actor = actor {
            counter.incrementAndGet()
            barrier.await()
            react {
                counter.incrementAndGet()
                barrier.await()
                react {
                    counter.incrementAndGet()
                    barrier.await()
                }
            }
        }.start()

        barrier.await()
        assertEquals 1, counter.intValue()

        actor << 'message'
        barrier.await()
        assertEquals 2, counter.intValue()

        actor << 'message'
        barrier.await()
        assertEquals 3, counter.intValue()
    }

    public void testReactWithBufferedMessages() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final AbstractPooledActor actor = actor {
            barrier.await()
            react {
                counter.incrementAndGet()
                barrier.await()
                barrier.await()
                react {
                    counter.incrementAndGet()
                    barrier.await()
                    barrier.await()
                    react {
                        counter.incrementAndGet()
                        barrier.await()
                    }
                }
            }
        }.start()

        actor.send('message')
        actor.send('message')
        actor.send('message')
        barrier.await()
        barrier.await()
        assertEquals 1, counter.intValue()
        barrier.await()

        barrier.await()
        assertEquals 2, counter.intValue()
        barrier.await()

        barrier.await()
        assertEquals 3, counter.intValue()
    }

    public void testReactWithDelayedMessages() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final AbstractPooledActor actor = actor {
            react {
                counter.incrementAndGet()
                barrier.await()
            }
        }.start()

        Thread.sleep(1000)
        actor.send('message')
        barrier.await()
        assertEquals 1, counter.intValue()
    }
}