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

import groovyx.gpars.group.NonDaemonPGroup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

public class SyncDataflowVariableDefaultTest extends GroovyTestCase {

    public void testVariable() {
        final SyncDataflowVariable variable = new SyncDataflowVariable()
        variable << 10
        assert 10 == variable.val
        assert 10 == variable.val

        shouldFail(IllegalStateException) {
            variable << 20
        }

        assert 10 == variable.val
    }

    public void testGet() {
        final SyncDataflowVariable variable = new SyncDataflowVariable()
        variable << 10
        assert 10 == variable.get()
        assert 10 == variable.get()
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    public void testTimeoutGet() {
        final SyncDataflowVariable variable = new SyncDataflowVariable()
        shouldFail(TimeoutException) {
            variable.get(1, TimeUnit.SECONDS)
        }
        variable << 10
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
        assert 10 == variable.get()
    }

    public void testGetException() {
        final SyncDataflowVariable variable = new SyncDataflowVariable()
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
        final SyncDataflowVariable variable = new SyncDataflowVariable()
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
        final SyncDataflowVariable variable = new SyncDataflowVariable()
        def group = new NonDaemonPGroup(2)
        group.blockingActor {
            variable << 10
        }

        final CountDownLatch latch = new CountDownLatch(1)
        List<Integer> result = []
        group.blockingActor {
            result << variable.val
            result << variable.val
            latch.countDown()
        }
        latch.await()
        assert 10 == result[0]
        assert 10 == result[1]
        group.shutdown()
    }

    public void testBlockedRead() {
        final SyncDataflowVariable<Integer> variable = new SyncDataflowVariable<Integer>()
        int result = 0
        final CountDownLatch latch = new CountDownLatch(1)

        def group = new NonDaemonPGroup(2)
        group.blockingActor {
            result = variable.val
            latch.countDown()
        }
        group.blockingActor {
            Thread.sleep 3000
            variable << 10
        }

        assert 10 == variable.val
        latch.await()
        assert 10 == result
        group.shutdown()
    }

    public void testNonBlockedRead() {
        final SyncDataflowVariable<Integer> variable = new SyncDataflowVariable<Integer>()
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CountDownLatch latch = new CountDownLatch(1)

        int result = 0
        def group = new NonDaemonPGroup(2)
        group.blockingActor {
            barrier.await()
            result = variable.val
            latch.countDown()
        }
        group.blockingActor {
            variable << 10
            barrier.await()
        }

        barrier.await()
        assert 10 == variable.val
        latch.await()
        assert 10 == result
        group.shutdown()
    }

    public void testToString() {
        final SyncDataflowVariable<Integer> variable = new SyncDataflowVariable<Integer>()
        assert 'SyncDataflowVariable(value=null)' == variable.toString()
        variable << 10
        assert 'SyncDataflowVariable(value=10)' == variable.toString()
        assert 'SyncDataflowVariable(value=10)' == variable.toString()
    }

    public void testVariableBlockedBoundHandler() {
        final SyncDataflowVariable<Integer> variable = new SyncDataflowVariable<Integer>()
        def result = new SyncDataflowVariable()

        variable >> {
            result << variable.val
        }
        def group = new NonDaemonPGroup(2)
        group.blockingActor {
            variable << 10
        }

        assert 10 == variable.val
        assert 10 == result.val
        group.shutdown()
    }

    public void testVariableNonBlockedBoundHandler() {
        final SyncDataflowVariable variable = new SyncDataflowVariable()
        variable << 10

        def result = new SyncDataflowVariable()

        variable >> {
            result << it
        }
        assert 10 == result.val
    }

    public void testVariablePoll() {
        final SyncDataflowVariable<Integer> variable = new SyncDataflowVariable<Integer>()
        def result = new SyncDataflowVariable()

        assert variable.poll() == null
        assert variable.poll() == null
        variable >> {
            result << variable.poll()
        }
        def group = new NonDaemonPGroup(2)
        group.blockingActor {
            variable << 10
        }

        assert 10 == variable.val
        assert 10 == result.val
        assert 10 == result.poll().val
        group.shutdown()
    }

    public void testJoin() {
        final SyncDataflowVariable variable = new SyncDataflowVariable()

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
        final SyncDataflowVariable variable = new SyncDataflowVariable()

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
        final SyncDataflowVariable variable = new SyncDataflowVariable()
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
        final SyncDataflowVariable variable = new SyncDataflowVariable()
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
