package org.gparallelizer.dataflow

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.CountDownLatch

public class DataFlowVariableTest extends GroovyTestCase {

    public void testVariable() {
        final DataFlowVariable variable = new DataFlowVariable()
        variable << 10
        assertEquals 10, variable.val
        assertEquals 10, variable.val

        shouldFail(IllegalStateException) {
            variable << 20
        }

        shouldFail(IllegalStateException) {
            final def v = new DataFlowVariable()
            v << 1
            variable << v
        }
        assertEquals 10, variable.val
    }

    public void testVariableFromThread() {
        final DataFlowVariable variable = new DataFlowVariable()
        DataFlow.start {
            variable << 10
        }

        final CountDownLatch latch = new CountDownLatch(1)
        volatile List<Integer> result = []
        DataFlow.start {
            result << variable.val
            result << variable.val
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

        DataFlow.start {
            result = variable.val
            latch.countDown()
        }
        DataFlow.start {
            Thread.sleep 3000
            variable << 10
        }

        assertEquals 10, variable.val
        latch.await()
        assertEquals 10, result
    }

    public void testNonBlockedRead() {
        final DataFlowVariable<Integer> variable = new DataFlowVariable<Integer>()
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CountDownLatch latch = new CountDownLatch(1)

        volatile int result = 0
        DataFlow.start {
            barrier.await()
            result = variable.val
            latch.countDown()
        }
        DataFlow.start {
            variable << 10
            barrier.await()
        }

        barrier.await()
        assertEquals 10, variable.val
        latch.await()
        assertEquals 10, result
    }

    public void testToString() {
        final DataFlowVariable<Integer> variable = new DataFlowVariable<Integer>()
        assertEquals 'DataFlowVariable(value=null)', variable.toString()
        variable << 10
        assertEquals 'DataFlowVariable(value=10)', variable.toString()
        assertEquals 'DataFlowVariable(value=10)', variable.toString()
    }
}