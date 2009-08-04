package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import org.gparallelizer.dataflow.DataFlowVariable

public class NullMessageTest extends GroovyTestCase{
    public void testNullMesage() {
        volatile def result = ''
        final def latch = new CountDownLatch(1)
        final AbstractThreadActor actor = Actors.oneShotActor {
            receive {
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
        final AbstractThreadActor actor = Actors.oneShotActor {
            receive {
                result = it
                latch.countDown()
            }
        }.start()
        Actors.actor {
            actor << null
            latch.await()
            stop()
        }.start()
        latch.await()
        assertNull result
    }

    public void testNullMesageFromActorWithReply() {
        final def result = new DataFlowVariable()
        final AbstractThreadActor actor = Actors.oneShotActor {
            receive {
                reply 10
            }
        }.start()
        Actors.oneShotActor {
            actor << null
            receive {
                result << it
            }
        }.start()
        assertEquals 10, result.val
    }
}