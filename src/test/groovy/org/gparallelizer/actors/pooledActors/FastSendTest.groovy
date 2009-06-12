package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch

public class FastSendTest extends GroovyTestCase {

    public void testFastSend() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch replyLatch = new CountDownLatch(1)

        final AbstractPooledActor actor = PooledActors.actor {
            disableSendingReplies()
            react {
                try {
                    reply 'Message22'
                } catch (Exception e) {
                    exception1 = e
                }
                try {
                    it.reply 'Message23'
                } catch (Exception e) {
                    exception2 = e
                }
                enableSendingReplies()
                replyLatch.countDown()
                react {
                    replyIfExists 'Message'
                    it.replyIfExists 'Message'
                    latch.countDown()
                }
            }
        }.start()

        actor << 'Message21'
        replyLatch.await()
        actor << 'Enabled message'
        latch.await()
        assertNotNull exception1
        assert exception1 instanceof IllegalStateException
        assertNotNull exception2
        assert exception2 instanceof MissingMethodException
    }

    public void testFastSendFromActor() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch replyLatch = new CountDownLatch(1)

        final AbstractPooledActor actor = PooledActors.actor {
            disableSendingReplies()

            react {
                try {
                    reply 'Message32'
                } catch (Exception e) {
                    exception1 = e
                }
                try {
                    it.reply 'Message33'
                } catch (Exception e) {
                    exception2 = e
                }
                enableSendingReplies()
                replyLatch.countDown()
                react {
                    replyIfExists 'Message'
                    it.replyIfExists 'Message'
                }
            }
        }.start()

        PooledActors.actor {
            actor << 'Message31'
            replyLatch.await()
            actor << 'Enabled message'
            react {a, b->
                latch.countDown()
            }
        }.start()

        latch.await()
        assertNotNull exception1
        assert exception1 instanceof IllegalStateException
        assertNotNull exception2
        assert exception2 instanceof MissingMethodException
    }
}