package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.gparallelizer.actors.BoundedActor

/**
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */

public class BoundedActorTest extends GroovyTestCase{
    public void testDefaultMessaging() {
        BoundedtTestActor actor=new BoundedtTestActor(2)
        actor.start()
        actor.send "Message"
        actor.latch.await(30, TimeUnit.SECONDS)
        assert actor.flag.get()
    }
}

class BoundedtTestActor extends BoundedActor {
    final AtomicBoolean flag = new AtomicBoolean(false)
    final CountDownLatch latch = new CountDownLatch(1)

    def BoundedtTestActor(final int capacity) {
        super(capacity);
    }

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
