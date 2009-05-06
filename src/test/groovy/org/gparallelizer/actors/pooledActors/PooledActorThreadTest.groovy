package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.Actor

public class PooledActorThreadTest extends GroovyTestCase {
    public void testActorThread() {
        volatile boolean flag1 = false
        volatile boolean flag2 = false
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor
        actor = PooledActors.actor {
            flag1=isActorThread()
            react {
                flag2=isActorThread()
                latch.countDown()
            }

        }.start()

        assertFalse actor.isActorThread()
        actor.send 'Message'
        latch.await()
        assert flag1
        assert flag2
    }
}