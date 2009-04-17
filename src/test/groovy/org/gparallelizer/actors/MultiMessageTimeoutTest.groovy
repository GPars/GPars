package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CyclicBarrier

public class MultiMessageTimeoutTest extends GroovyTestCase {
    public void testReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            receive(10, TimeUnit.SECONDS) {a, b, c ->
                result = a + b + c
                latch.countDown()
            }
        }.start()

        actor.send 2
        actor.send 3
        actor.send 4

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 9, result
    }

    public void testReceiveZeroTimeout() {
        CountDownLatch latch = new CountDownLatch(1)
        CyclicBarrier barrier = new CyclicBarrier(2)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            barrier.await()
            receive(0, TimeUnit.SECONDS) {a, b, c ->
                result = a + b + c
                latch.countDown()
            }
        }.start()

        actor.send 2
        actor.send 3
        actor.send 4
        barrier.await()

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 9, result
    }

    public void testReceivePassedZeroTimeout() {
        CountDownLatch latch = new CountDownLatch(1)
        CyclicBarrier barrier = new CyclicBarrier(2)
        volatile List result = []

        def actor = Actors.oneShotActor {
            barrier.await()
            receive(0, TimeUnit.SECONDS) {a, b, c ->
                result << a
                result << b
                result << c
                latch.countDown()
            }
        }.start()

        actor.send 2
        barrier.await()
        latch.await(30, TimeUnit.SECONDS)
        assertEquals([2, null, null], result)
    }

    public void testReceivePassedNonZeroTimeout() {
        CountDownLatch latch = new CountDownLatch(1)
        CyclicBarrier barrier = new CyclicBarrier(2)
        volatile List result = []

        def actor = Actors.oneShotActor {
            barrier.await()
            receive(1, TimeUnit.SECONDS) {a, b, c ->
                result << a
                result << b
                result << c
                latch.countDown()
            }
        }.start()

        actor.send 2
        barrier.await()
        latch.await(30, TimeUnit.SECONDS)
        assertEquals([2, null, null], result)
    }

    public void testReceiveNonZeroTimeout() {
        CountDownLatch latch = new CountDownLatch(1)
        CyclicBarrier barrier = new CyclicBarrier(2)
        volatile List result = []

        def actor = Actors.oneShotActor {
            barrier.await()
            receive(2, TimeUnit.SECONDS) {a, b, c ->
                result << a
                result << b
                result << c
                latch.countDown()
            }
        }.start()

        actor.send 2
        actor.send 3
        actor.send 4
        barrier.await()
        latch.await(30, TimeUnit.SECONDS)
        assertEquals([2, 3, 4], result)
    }

    public void testNoMessageReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            receive(10, TimeUnit.SECONDS) {->
                latch.countDown()
            }
        }.start()

        actor.send 2

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 0, result
    }

    public void testNoMessageReceiveZeroTimeout() {
        CyclicBarrier barrier = new CyclicBarrier(2)
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            barrier.await()
            receive(0, TimeUnit.SECONDS) {->
                latch.countDown()
            }
        }.start()

        actor.send 2
        barrier.await()

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 0, result
    }

    public void testNoMessageReceivePassedZeroTimeout() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            receive(0, TimeUnit.SECONDS) {->
                latch.countDown()
            }
        }.start()


        latch.await(30, TimeUnit.SECONDS)
        assertEquals 0, result
    }

    public void testDefaultMessageReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            receive(10, TimeUnit.SECONDS) {
                latch.countDown()
            }
        }.start()

        actor.send 2

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 0, result
    }

    public void testArrayReceive() {
        CountDownLatch latch = new CountDownLatch(1)
        volatile int result = 0

        def actor = Actors.oneShotActor {
            receive(10, TimeUnit.SECONDS) {a, b, c ->
                result = a[2] + b + c
                latch.countDown()
            }
        }.start()

        actor.send([2, 10, 20])
        actor.send 3
        actor.send 4

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 27, result
    }
}