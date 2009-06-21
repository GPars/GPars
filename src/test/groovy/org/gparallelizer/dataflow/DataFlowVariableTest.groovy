package org.gparallelizer.dataflow

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.CountDownLatch

public class DataFlowVariableTest extends GroovyTestCase {

    public void testVariable() {
        final DataFlowVariable variable = new DataFlowVariable()
        variable << 10
        assertEquals 10, ~variable
        assertEquals 10, ~variable

        shouldFail(IllegalStateException) {
            variable << 20
        }

        shouldFail(IllegalStateException) {
            final def v = new DataFlowVariable()
            v << 1
            variable << v
        }
        assertEquals 10, ~variable
    }

    public void testVariableFromThread() {
        final DataFlowVariable variable = new DataFlowVariable()
        DataFlow.thread {
            variable << 10
        }

        final CountDownLatch latch = new CountDownLatch(1)
        volatile List<Integer> result = []
        DataFlow.thread {
            result << ~variable
            result << ~variable
            latch.countDown()
        }
        latch.await()
        assertEquals 10, result[0]
        assertEquals 10, result[1]
    }

    public void testBlockedRead() {
        final DataFlowVariable<Integer> variable = new DataFlowVariable<Integer>()
        volatile int result = 0
        final CountDownLatch latch = new CountDownLatch(1)

        DataFlow.thread {
            result = ~variable
            latch.countDown()
        }
        DataFlow.thread {
            Thread.sleep 3000
            variable << 10
        }

        assertEquals 10, ~variable
        latch.await()
        assertEquals 10, result
    }

    public void testNonBlockedRead() {
        final DataFlowVariable<Integer> variable = new DataFlowVariable<Integer>()
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CountDownLatch latch = new CountDownLatch(1)

        volatile int result = 0
        DataFlow.thread {
            barrier.await()
            result = ~variable
            latch.countDown()
        }
        DataFlow.thread {
            variable << 10
            barrier.await()
        }

        barrier.await()
        assertEquals 10, ~variable
        latch.await()
        assertEquals 10, result
    }
}