package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch
import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.actors.pooledActors.PooledActors.*

public class NullMessageTest extends GroovyTestCase {
    public void testNullMesage() {
        volatile def result = ''
        final def latch = new CountDownLatch(1)
        final AbstractPooledActor actor = actor {
            react {
                result = it
                latch.countDown()
            }
        }.start()
        actor << null
        latch.await()
        assertNull result
    }

    public void testNullMesageFromActor() {
        volatile def result = ''
        final def latch = new CountDownLatch(1)
        final AbstractPooledActor actor = actor {
            react {
                result = it
                latch.countDown()
            }
        }.start()
        PooledActors.actor {
            actor << null
            latch.await()
        }.start()
        latch.await()
        assertNull result
    }

    public void testNullMesageFromActorWithReply() {
        final def result = new DataFlowVariable()
        final AbstractPooledActor actor = actor {
            react {
                reply 10
            }
        }.start()
        PooledActors.actor {
            actor << null
            react {
                result << it
            }
        }.start()
        assertEquals 10, result.val
    }
}