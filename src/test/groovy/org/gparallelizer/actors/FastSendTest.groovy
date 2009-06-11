package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch


public class FastSendTest extends GroovyTestCase {

    public void testFastSend() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.oneShotActor {
            disableSendingReplies()
            receive {
                try {
                    reply 'Message'
                } catch (Exception e) {
                    exception1 = e
                }
                try {
                    it.reply 'Message'
                } catch (Exception e) {
                    exception2 = e
                }
                latch.countDown()
            }
        }.start()

        actor << 'Message'
        latch.await()
        assertNotNull exception1
        assert exception1 instanceof IllegalStateException
        assertNotNull exception2
        assert exception2 instanceof MissingMethodException
    }

    public void testFastSendFromActor() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.oneShotActor {
            disableSendingReplies()
            receive {
                try {
                    reply 'Message'
                } catch (Exception e) {
                    exception1 = e
                }
                try {
                    it.reply 'Message'
                } catch (Exception e) {
                    exception2 = e
                }
                latch.countDown()
            }
        }.start()

        Actors.oneShotActor {
            actor << 'Message'
            latch.await()

        }.start()

        latch.await()
        assertNotNull exception1
        assert exception1 instanceof IllegalStateException
        assertNotNull exception2
        assert exception2 instanceof MissingMethodException
    }
}