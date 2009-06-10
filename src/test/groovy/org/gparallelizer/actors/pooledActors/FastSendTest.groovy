package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch

public class FastSendTest extends GroovyTestCase {

    public void testFastSend() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)

        final AbstractPooledActor actor = PooledActors.actor {
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

        actor.fastSend 'Message11'
        latch.await()
        assertNotNull exception1
        assertNotNull exception2
        println exception1
        println exception2
    }

    public void testFastSendFromActor() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)

        final AbstractPooledActor actor = PooledActors.actor {
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
            actor.fastSend 'Message11'
            latch.await()

        }.start()

        latch.await()
        assertNotNull exception1
        assertNotNull exception2
        println exception1
        println exception2
    }
}