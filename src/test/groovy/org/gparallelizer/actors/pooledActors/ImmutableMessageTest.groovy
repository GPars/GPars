package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch

public class ImmutableMessageTest extends GroovyTestCase {

    public void testSend() {
        volatile String result
        final CountDownLatch latch = new CountDownLatch(1)

        final AbstractPooledActor bouncer = PooledActors.actor {
            react {
                it.reply new TestMessage(value : it.value)
            }
        }.start()

        PooledActors.actor {
            bouncer << new TestMessage(value : 'Value')
            react {
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