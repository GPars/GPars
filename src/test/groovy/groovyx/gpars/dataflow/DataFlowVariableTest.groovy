// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.dataflow

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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

    public void testGet() {
        final DataFlowVariable variable = new DataFlowVariable()
        variable << 10
        assertEquals 10, variable.get()
        assertEquals 10, variable.get()
        assertEquals 10, variable.get(10, TimeUnit.SECONDS)
    }

    public void testTimeoutGet() {
        final DataFlowVariable variable = new DataFlowVariable()
        shouldFail(TimeoutException) {
            variable.get(1, TimeUnit.SECONDS)
        }
        variable << 10
        assertEquals 10, variable.get(10, TimeUnit.SECONDS)
        assertEquals 10, variable.get()
    }

    public void testGetException() {
        final DataFlowVariable variable = new DataFlowVariable()
        variable << new Exception('test')
        shouldFail(Exception) {
            variable.get()
        }
        shouldFail(Exception) {
            variable.get()
        }
        shouldFail(Exception) {
            variable.get(10, TimeUnit.SECONDS)
        }
    }

    public void testGetRuntimeException() {
        final DataFlowVariable variable = new DataFlowVariable()
        variable << new RuntimeException('test')
        shouldFail(RuntimeException) {
            variable.get()
        }
        shouldFail(RuntimeException) {
            variable.get()
        }
        shouldFail(RuntimeException) {
            variable.get(10, TimeUnit.SECONDS)
        }
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

    public void testVariableBlockedBoundHandler() {
        final DataFlowVariable<Integer> variable = new DataFlowVariable<Integer>()
        volatile def result = new DataFlowVariable()

        variable >> {
            result << variable.val
        }
        DataFlow.start {
            variable << 10
        }

        assertEquals 10, variable.val
        assertEquals 10, result.val
    }

    public void testVariableNonBlockedBoundHandler() {
        final DataFlowVariable variable = new DataFlowVariable()
        variable << 10

        volatile def result = new DataFlowVariable()

        variable >> {
            result << it
        }
        assertEquals 10, result.val
    }

    public void testVariablePoll() {
        final DataFlowVariable<Integer> variable = new DataFlowVariable<Integer>()
        volatile def result = new DataFlowVariable()

        assert variable.poll() == null
        assert variable.poll() == null
        variable >> {
            result << variable.poll()
        }
        DataFlow.start {
            variable << 10
        }

        assertEquals 10, variable.val
        assertEquals 10, result.val
        assertEquals 10, result.poll().val
    }

    public void testJoin() {
        final DataFlowVariable variable = new DataFlowVariable()

        def t1 = System.currentTimeMillis()

        Thread.start {
            sleep 1000
            variable << 10
        }

        variable.join()
        assert 10 == variable.val

        variable.join()
        assert 10 == variable.val

        assert System.currentTimeMillis() - t1 < 60000
    }

    public void testTimedJoin() {
        final DataFlowVariable variable = new DataFlowVariable()

        def t1 = System.currentTimeMillis()

        final CyclicBarrier barrier = new CyclicBarrier(2)

        Thread.start {
            barrier.await()
            variable << 10
        }
        variable.join(10, TimeUnit.MILLISECONDS)
        assert !variable.isBound()
        barrier.await()

        variable.join(10, TimeUnit.MINUTES)
        assert 10 == variable.val

        variable.join(10, TimeUnit.MINUTES)
        assert 10 == variable.val

        assert System.currentTimeMillis() - t1 < 60000
    }

    public void testEqualValueRebind() {
        final DataFlowVariable variable = new DataFlowVariable()
        variable.bind([1, 2, 3])
        variable.bind([1, 2, 3])
        variable.bindSafely([1, 2, 3])
        variable << [1, 2, 3]
        shouldFail(IllegalStateException) {
            variable.bind([1, 2, 3, 4, 5])
        }
        shouldFail(IllegalStateException) {
            variable << [1, 2, 3, 4, 5]
        }
        shouldFail(IllegalStateException) {
            variable.bindUnique([1, 2, 3])
        }
        shouldFail(IllegalStateException) {
            variable.bindUnique([1, 2, 3, 4, 5])
        }
    }

    public void testNullValueRebind() {
        final DataFlowVariable variable = new DataFlowVariable()
        variable.bind(null)
        variable.bind(null)
        variable.bindSafely(null)
        variable << null
        shouldFail(IllegalStateException) {
            variable.bind(10)
        }
        shouldFail(IllegalStateException) {
            variable << 20
        }
        shouldFail(IllegalStateException) {
            variable.bindUnique(null)
        }
        shouldFail(IllegalStateException) {
            variable.bindUnique(30)
        }
    }
}
