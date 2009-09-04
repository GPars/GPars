package org.gparallelizer.dataflow

import static org.gparallelizer.dataflow.DataFlow.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

public class DataFlowTest extends GroovyTestCase {

    public void testSimpleAssignment() {
        DataFlowVariable<Integer> x = new DataFlowVariable()
        DataFlowVariable<Integer> y = new DataFlowVariable()
        DataFlowVariable<Integer> z = new DataFlowVariable()

        volatile def result = 0
        final def latch = new CountDownLatch(1)

        start {
            z << x.val + y.val
            result = z.val
            latch.countDown()
        }

        start {x << 40}
        start {y << 2}

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 42, result
    }

    List<Integer> ints(int n, int max) {
        if (n == max) return []
        else return [n, * ints(n + 1, max)]
    }

    List<Integer> sum(int s, List<Integer> stream) {
        switch (stream.size()) {
            case 0: return [s]
            default:
                return [s, * sum(stream[0] + s, stream.size() > 1 ? stream[1..-1] : [])]
        }
    }

    public void testListAssignment() {
        def x = new DataFlowVariable<List<Integer>>()
        def y = new DataFlowVariable<List<Integer>>()

        volatile def result = 0
        final def latch = new CountDownLatch(1)

        start { x << ints(0, 10) }
        start { y << sum(0, x.val) }
        start {
            result = y.val
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assertEquals([0, 0, 1, 3, 6, 10, 15, 21, 28, 36, 45], result)
    }
}