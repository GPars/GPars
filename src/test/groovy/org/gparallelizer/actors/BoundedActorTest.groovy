package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.gparallelizer.actors.BoundThreadActor

/**
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */

public class BoundActorTest extends GroovyTestCase{
    public void testDefaultMessaging() {
        BoundTestActor actor=new BoundTestActor(2)
        actor.start()
        actor.send "Message"
        actor.latch.await(30, TimeUnit.SECONDS)
        assert actor.flag.get()
    }
}

class BoundTestActor extends BoundThreadActor {
    final AtomicBoolean flag = new AtomicBoolean(false)
    final CountDownLatch latch = new CountDownLatch(1)

    def BoundTestActor(final int capacity) {
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
