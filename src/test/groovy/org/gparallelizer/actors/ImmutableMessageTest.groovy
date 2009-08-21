package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.Actors

public class ImmutableMessageTest extends GroovyTestCase {
    public void testSend() {
        volatile String result
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor bouncer = Actors.oneShotActor {
            receive {
                it.reply new TestMessage(value : it.value)
            }
        }.start()

        Actors.oneShotActor {
            bouncer << new TestMessage(value : 'Value')
            receive {
                result = it.value
                latch.countDown()
            }
        }.start()

        latch.await()
        assertEquals 'Value', result

    }
}

final @Immutable class TestMessage {
    String value
}