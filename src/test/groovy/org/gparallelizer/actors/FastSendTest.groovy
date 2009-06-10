package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch


//todo enable
public abstract class FastSendTest extends GroovyTestCase {

    public void testFastSend() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.oneShotActor {
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

        actor.fastSend 'Message'
        latch.await()
        assertNotNull exception1
        assertNotNull exception2
        println exception1
    }

    public void testFastSendFromActor() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.oneShotActor {
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
            actor.fastSend 'Message'
            latch.await()

        }.start()

        latch.await()
        assertNotNull exception1
        assertNotNull exception2
        println exception1
    }
}