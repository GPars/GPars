package org.gparallelizer.actors

import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.CyclicBarrier

public class CrossReplyTest extends GroovyTestCase {

    public void testReplyToThreadBound() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = PooledActors.actor {
            react {
                reply it + 1
            }
        }
        incrementor.start()

        Actor actor = Actors.actor {
            incrementor.send 2
            receive {
                result = it
                barrier.await()
                stop()
            }
        }
        actor.start()

        barrier.await()
        incrementor.stop()
        assertEquals 3, result
    }

    public void testMessageReplyToThreadBound() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = PooledActors.actor {
            react {
                it.reply it + 1
            }
        }
        incrementor.start()

        Actor actor = Actors.actor {
            incrementor.send 2
            receive {
                result = it
                barrier.await()
                stop()
            }
        }
        actor.start()

        barrier.await()
        assertEquals 3, result
    }

    public void testReplyToEventDriven() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = Actors.actor {
            receive {
                reply it + 1
            }
            stop()
        }
        incrementor.start()

        Actor actor = PooledActors.actor {
            incrementor.send 2
            react {
                result = it
                barrier.await()
            }
        }
        actor.start()

        barrier.await()
        incrementor.stop()
        assertEquals 3, result
    }

    public void testMessageReplyToEventDriven() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = Actors.actor {
            receive {
                it.reply it + 1
            }
            stop()
        }
        incrementor.start()

        Actor actor = PooledActors.actor {
            incrementor.send 2
            react {
                result = it
                barrier.await()
            }
        }
        actor.start()

        barrier.await()
        assertEquals 3, result
    }

}