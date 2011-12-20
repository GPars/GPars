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

import groovyx.gpars.actor.Actors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

public class DataflowVariableTest extends GroovyTestCase {

    public void testVariable() {
        final DataflowVariable variable = new DataflowVariable()
        variable << 10
        assert 10 == variable.val
        assert 10 == variable.val

        shouldFail(IllegalStateException) {
            variable << 20
        }

        shouldFail(IllegalStateException) {
            final def v = new DataflowVariable()
            v << 1
            variable << v
        }
        assert 10 == variable.val
    }

    public void testGet() {
        final DataflowVariable variable = new DataflowVariable()
        variable << 10
        assert 10 == variable.get()
        assert 10 == variable.get()
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    public void testTimeoutGet() {
        final DataflowVariable variable = new DataflowVariable()
        shouldFail(TimeoutException) {
            variable.get(1, TimeUnit.SECONDS)
        }
        variable << 10
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
        assert 10 == variable.get()
    }

    public void testGetException() {
        final DataflowVariable variable = new DataflowVariable()
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
        final DataflowVariable variable = new DataflowVariable()
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
        final DataflowVariable variable = new DataflowVariable()
        Actors.blockingActor {
            variable << 10
        }

        final CountDownLatch latch = new CountDownLatch(1)
        List<Integer> result = []
        Actors.blockingActor {
            result << variable.val
            result << variable.val
            latch.countDown()
        }
        latch.await()
        assert 10 == result[0]
        assert 10 == result[1]
    }

    public void testBlockedRead() {
        final DataflowVariable<Integer> variable = new DataflowVariable<Integer>()
        int result = 0
        final CountDownLatch latch = new CountDownLatch(1)

        Actors.blockingActor {
            result = variable.val
            latch.countDown()
        }
        Actors.blockingActor {
            Thread.sleep 3000
            variable << 10
        }

        assert 10 == variable.val
        latch.await()
        assert 10 == result
    }

    public void testNonBlockedRead() {
        final DataflowVariable<Integer> variable = new DataflowVariable<Integer>()
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CountDownLatch latch = new CountDownLatch(1)

        int result = 0
        Actors.blockingActor {
            barrier.await()
            result = variable.val
            latch.countDown()
        }
        Actors.blockingActor {
            variable << 10
            barrier.await()
        }

        barrier.await()
        assert 10 == variable.val
        latch.await()
        assert 10 == result
    }

    public void testToString() {
        final DataflowVariable<Integer> variable = new DataflowVariable<Integer>()
        assert 'DataflowVariable(value=null)' == variable.toString()
        variable << 10
        assert 'DataflowVariable(value=10)' == variable.toString()
        assert 'DataflowVariable(value=10)' == variable.toString()
    }

    public void testVariableBlockedBoundHandler() {
        final DataflowVariable<Integer> variable = new DataflowVariable<Integer>()
        def result = new DataflowVariable()

        variable >> {
            result << variable.val
        }
        Actors.blockingActor {
            variable << 10
        }

        assert 10 == variable.val
        assert 10 == result.val
    }

    public void testVariableNonBlockedBoundHandler() {
        final DataflowVariable variable = new DataflowVariable()
        variable << 10

        def result = new DataflowVariable()

        variable >> {
            result << it
        }
        assert 10 == result.val
    }

    public void testVariablePoll() {
        final DataflowVariable<Integer> variable = new DataflowVariable<Integer>()
        final def result = new DataflowVariable()

        assert variable.poll() == null
        assert variable.poll() == null
        variable >> {
            result << variable.poll()
        }
        Actors.blockingActor {
            variable << 10
        }

        assert 10 == variable.val
        assert 10 == result.val
        assert 10 == result.poll().val
    }

    public void testJoin() {
        final DataflowVariable variable = new DataflowVariable()

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
        final DataflowVariable variable = new DataflowVariable()

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
        final DataflowVariable variable = new DataflowVariable()
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
        final DataflowVariable variable = new DataflowVariable()
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
