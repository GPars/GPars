package org.gparallelizer.actors

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public class SynchronousActorTest extends GroovyTestCase {
    public void testDefaultMessaging() {
        SynchronoustTestActor actor=new SynchronoustTestActor()
        actor.start()
        actor.send "Message"
        actor.latch.await(30, TimeUnit.SECONDS)
        assert actor.flag.get()
    }
}

class SynchronoustTestActor extends SynchronousActor {
    final AtomicBoolean flag = new AtomicBoolean(false)
    final CountDownLatch latch = new CountDownLatch(1)

    @Override protected void act() {
        receive {
            flag.set true
            latch.countDown()

            stop()
        }
    }

    public String getThreadName() {
        return getActorThread().name
    }
}
