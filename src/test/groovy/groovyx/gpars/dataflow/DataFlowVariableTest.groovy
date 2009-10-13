//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package groovyx.gpars.dataflow

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

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

}
