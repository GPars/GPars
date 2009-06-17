package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

public class DeliveryErrorTest extends GroovyTestCase {
    public void testSuccessfulMessages() {
        volatile boolean flag = false
        CountDownLatch latch = new CountDownLatch(1)

        final DefaultThreadActor actor = Actors.oneShotActor {
            final def a = receive()
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()
        
        def message = 1
        message.metaClass.onDeliveryError = {->
            flag = true
        }
        actor << message

        latch.await()
        assertFalse flag 
    }

    public void testFailedMessages() {
        volatile boolean flag1 = false
        volatile boolean flag2 = false
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DefaultThreadActor actor = Actors.oneShotActor {
            final def a = receive()
            barrier.await()
        }

        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        def message1 = 1
        message1.metaClass.onDeliveryError = {->
            flag1 = true
        }

        def message2 = 2
        message2.metaClass.onDeliveryError = {->
            flag2 = true
        }
        actor << message1
        actor << message2
        barrier.await()

        latch.await()
        assertFalse flag1
        assert flag2
    }

    public void testFailedMessagesOnException() {
        volatile boolean flag1 = false
        volatile boolean flag2 = false
        CountDownLatch latch = new CountDownLatch(1)
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DefaultThreadActor actor = Actors.oneShotActor {
            final def a = receive()
            barrier.await()
            if (true) throw new RuntimeException('test')
        }

        actor.metaClass.onException = {}
        actor.metaClass.afterStop = {
            latch.countDown()
        }

        actor.start()

        def message1 = 1
        message1.metaClass.onDeliveryError = {->
            flag1 = true
        }

        def message2 = 2
        message2.metaClass.onDeliveryError = {->
            flag2 = true
        }
        actor << message1
        actor << message2
        barrier.await()

        latch.await()
        assertFalse flag1
        assert flag2
    }
}