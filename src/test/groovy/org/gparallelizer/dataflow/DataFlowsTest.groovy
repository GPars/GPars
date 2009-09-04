package org.gparallelizer.dataflow

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.CountDownLatch

public class DataFlowsTest extends GroovyTestCase {

    public void testValueAssignment() {
        final DataFlows data = new DataFlows()

        data.y = 'value'
        final def y = data.y
        assert y instanceof String
        assertEquals 'value', y
        assertEquals 'value', data.y

        shouldFail(IllegalStateException) {
            data.y = 20
        }
    }

    public void testDoubleAssignment() {
        final DataFlows data = new DataFlows()

        shouldFail(IllegalStateException) {
            data.x = 1
            data.x = 2
        }
        assertEquals 1, data.x
    }

    public void testVariableFromThread() {
        final DataFlows data = new DataFlows()

        DataFlow.start {
            data.variable = 10
        }

        final CountDownLatch latch = new CountDownLatch(1)
        volatile List<Integer> result = []
        DataFlow.start {
            result << data.variable
            result << data.variable
            latch.countDown()
        }
        latch.await()
        assertEquals 10, result[0]
        assertEquals 10, result[1]
    }

    public void testBlockedRead() {
        final DataFlows data = new DataFlows()

        volatile int result = 0
        final CountDownLatch latch = new CountDownLatch(1)

        DataFlow.start {
            result = data.variable
            latch.countDown()
        }
        DataFlow.start {
            Thread.sleep 3000
            data.variable = 10
        }

        assertEquals 10, data.variable
        latch.await()
        assertEquals 10, result
    }

    public void testNonBlockedRead() {
        final DataFlows data = new DataFlows()
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CountDownLatch latch = new CountDownLatch(1)

        volatile int result = 0
        DataFlow.start {
            barrier.await()
            result = data.variable
            latch.countDown()
        }
        DataFlow.start {
            data.variable = 10
            barrier.await()
        }

        barrier.await()
        assertEquals 10, data.variable
        latch.await()
        assertEquals 10, result
    }

      public void testIndexes() {
          final DataFlows data = new DataFlows()

          DataFlow.start {
            data[2] = data [0] - data [1]
          }
          DataFlow.start {
            data [1] = 5
          }
          DataFlow.start {
            data [0] = 7
          }
          assertEquals 2, data[2]
      }
}