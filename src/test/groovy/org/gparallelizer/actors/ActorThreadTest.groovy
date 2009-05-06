package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch

public class ActorThreadTest extends GroovyTestCase {

    public void testActorThread() {
        volatile boolean flag1 = false
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor
        actor = Actors.oneShotActor {
            receive()
            flag1=isActorThread()
            latch.countDown()

        }.start()

        assertFalse actor.isActorThread()
        actor.send 'Message'
        latch.await()
        assert flag1
    }
}