package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch

public class FastSendTest extends GroovyTestCase {

    public void testFastSend() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)

        final AbstractPooledActor actor = PooledActors.actor {
            disableSendingReplies()
            react {
                try {
                    reply 'Message2'
                } catch (Exception e) {
                    exception1 = e
                }
                try {
                    it.reply 'Message3'
                } catch (Exception e) {
                    exception2 = e
                }
                latch.countDown()
            }
        }.start()

        actor << 'Message11'
        latch.await()
        assertNotNull exception1
        assert exception1 instanceof IllegalStateException
        assertNotNull exception2
        assert exception2 instanceof MissingMethodException
    }

    public void testFastSendFromActor() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)

        final AbstractPooledActor actor = PooledActors.actor {
            disableSendingReplies()
            react {
                try {
                    reply 'Message2'
                } catch (Exception e) {
                    exception1 = e
                }
                try {
                    it.reply 'Message3'
                } catch (Exception e) {
                    exception2 = e
                }
                latch.countDown()
            }
        }.start()

        PooledActors.actor {
            actor << 'Message11'
            latch.await()

        }.start()

        latch.await()
        assertNotNull exception1
        assert exception1 instanceof IllegalStateException
        assertNotNull exception2
        assert exception2 instanceof MissingMethodException
    }
}