package org.gparallelizer.dataflow

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

public class ActorDataFlowVariableTest extends GroovyTestCase {

    public void testVariable() {
        final ActorBasedDataFlowVariable variable = new ActorBasedDataFlowVariable()
        variable << 10
        assertEquals 10, ~variable
        assertEquals 10, ~variable

        shouldFail(IllegalStateException) {
            variable << 20
        }

        shouldFail(IllegalStateException) {
            final def v = new ActorBasedDataFlowVariable()
            v << 1
            variable << v
        }
        assertEquals 10, ~variable
    }

    public void testVariableFromThread() {
        final ActorBasedDataFlowVariable variable = new ActorBasedDataFlowVariable()
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
        final ActorBasedDataFlowVariable<Integer> variable = new ActorBasedDataFlowVariable<Integer>()
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
        final ActorBasedDataFlowVariable<Integer> variable = new ActorBasedDataFlowVariable<Integer>()
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

    public void testExit() {
        final ActorBasedDataFlowVariable variable = new ActorBasedDataFlowVariable()
        DataFlow.thread {
            Thread.sleep 3000
            variable.shutdown()
        }

        shouldFail(IllegalStateException) {
            ~variable
        }
    }

    public void testDoubleShutdown() {
        final ActorBasedDataFlowVariable variable = new ActorBasedDataFlowVariable()
        variable.shutdown()
        shouldFail(IllegalStateException) {
            variable << 10
        }
        shouldFail(IllegalStateException) {
            ~variable
        }
        shouldFail(IllegalStateException) {
            variable.shutdown()
        }
    }

    public void testAssignedVariableAfterShutdown() {
        final ActorBasedDataFlowVariable variable = new ActorBasedDataFlowVariable()
        variable << 10
        ~variable
        variable.shutdown()
        shouldFail(IllegalStateException) {
            variable << 20
        }
        assertEquals 10, ~variable
        shouldFail(IllegalStateException) {
            variable.shutdown()
        }
    }

    public void testUnassignedVariableAfterShutdown() {
        final ActorBasedDataFlowVariable variable = new ActorBasedDataFlowVariable()

        DataFlow.thread {
            Thread.sleep 3000
            variable.shutdown()
        }

        shouldFail(IllegalStateException) {
            ~variable
        }
        shouldFail(IllegalStateException) {
            ~variable
        }
    }

    public void testUnassignedVariableShortlyAfterShutdown() {
        final ActorBasedDataFlowVariable variable = new ActorBasedDataFlowVariable()

        variable.shutdown()
        shouldFail(IllegalStateException) {
            ~variable
        }
        shouldFail(IllegalStateException) {
            ~variable
        }
    }

    public void testAssignedVariableShortlyAfterShutdown() {
        final ActorBasedDataFlowVariable variable = new ActorBasedDataFlowVariable()

        variable << 10
        variable.shutdown()
        ~variable
    }
}