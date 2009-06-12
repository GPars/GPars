package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch


public class FastSendTest extends GroovyTestCase {

    public void testFastSend() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch replyLatch = new CountDownLatch(1)

        final Actor actor = Actors.oneShotActor {
            disableSendingReplies()
            receive {
                try {
                    reply 'Message42'
                } catch (Exception e) {
                    exception1 = e
                }
                try {
                    it.reply 'Message43'
                } catch (Exception e) {
                    exception2 = e
                }
                enableSendingReplies()
                replyLatch.countDown()
                receive {
                    replyIfExists 'Message'
                    it.replyIfExists 'Message'
                }
                latch.countDown()
            }
        }.start()

        actor << 'Message41'
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

        final Actor actor = Actors.oneShotActor {
            disableSendingReplies()
            receive {
                try {
                    reply 'Message52'
                } catch (Exception e) {
                    exception1 = e
                }
                try {
                    it.reply 'Message53'
                } catch (Exception e) {
                    exception2 = e
                }
                enableSendingReplies()
                replyLatch.countDown()
                receive {
                    reply 'Message'
                    it.reply 'Message'
                }
            }
        }.start()

        Actors.oneShotActor {
            actor << 'Message51'
            replyLatch.await()
            actor << 'Enabled message'
            receive{a, b-> }
            latch.countDown()
        }.start()

        latch.await()
        assertNotNull exception1
        assert exception1 instanceof IllegalStateException
        assertNotNull exception2
        assert exception2 instanceof MissingMethodException
    }
}