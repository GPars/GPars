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

public class DataflowsTest extends GroovyTestCase {

    public void testValueAssignment() {
        final Dataflows data = new Dataflows()

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
        final Dataflows data = new Dataflows()

        shouldFail(IllegalStateException) {
            data.x = 1
            data.x = 2
        }
        assertEquals 1, data.x
    }

    public void testVariableFromThread() {
        final Dataflows data = new Dataflows()

        Actors.blockingActor {
            data.variable = 10
        }

        final CountDownLatch latch = new CountDownLatch(1)
        volatile List<Integer> result = []
        Actors.blockingActor {
            result << data.variable
            result << data.variable
            latch.countDown()
        }
        latch.await()
        assertEquals 10, result[0]
        assertEquals 10, result[1]
    }

    public void testBlockedRead() {
        final Dataflows data = new Dataflows()

        volatile int result = 0
        final CountDownLatch latch = new CountDownLatch(1)

        Actors.blockingActor {
            result = data.variable
            latch.countDown()
        }
        Actors.blockingActor {
            Thread.sleep 3000
            data.variable = 10
        }

        assertEquals 10, data.variable
        latch.await()
        assertEquals 10, result
    }

    public void testNonBlockedRead() {
        final Dataflows data = new Dataflows()
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final CountDownLatch latch = new CountDownLatch(1)

        volatile int result = 0
        Actors.blockingActor {
            barrier.await()
            result = data.variable
            latch.countDown()
        }
        Actors.blockingActor {
            data.variable = 10
            barrier.await()
        }

        barrier.await()
        assertEquals 10, data.variable
        latch.await()
        assertEquals 10, result
    }

    public void testIndexes() {
        final Dataflows data = new Dataflows()

        Actors.blockingActor {
            //noinspection GroovyAssignmentCanBeOperatorAssignment
            data[2] = data[0] - data[1]
        }
        Actors.blockingActor {
            data[1] = 5
        }
        Actors.blockingActor {
            data[0] = 7
        }
        assertEquals 2, data[2]
    }

    public void testValueRemoval() {
        final Dataflows data = new Dataflows()

        data.y = 'value1'
        shouldFail {
            data.y = 'value2'
        }
        data.remove('y')
        data.y = 'value3'

        final def y = data.y
        assert y instanceof String
        assertEquals 'value3', y
        assertEquals 'value3', data.y
    }

    public void testUnblockingAfterValueRemoval() {
        final Dataflows data = new Dataflows()
        final CyclicBarrier barrier = new CyclicBarrier(2)

        Actors.blockingActor {
            barrier.await()
            data.y = 'value'
        }

        Actors.blockingActor {
            Thread.sleep 1000
            data.remove('y')
            barrier.await()
        }

        final def y = data.y
        assertNull y

        y = data.y  //retry
        assert y instanceof String
        assertEquals 'value', y
        assertEquals 'value', data.y
    }

    public void testWhenValueBound() {
        final Dataflows data = new Dataflows()
        final def result1 = new DataflowVariable()
        final def result2 = new DataflowVariable()

        data.y {result1 << it }
        data.y = 'value'
        data.y {result2 << it }

        assert result1.val instanceof String
        assertEquals 'value', result1.val
        assert result2.val instanceof String
        assertEquals 'value', result2.val
        assertEquals 'value', data.y

        shouldFail(IllegalStateException) {
            data.y = 20
        }
    }

    public void testChainedWhenValueBound() {
        final Dataflows data = new Dataflows()
        final def result1 = new DataflowVariable()
        final def result2 = new DataflowVariable()
        final def result3 = new DataflowVariable()

        data.y {
            result3 << it
        }.x {
            result1 << data.y
            result2 << it
        }
        data.x = 'value2'
        data.y = 'value1'

        assertEquals 'value1', result1.val
        assertEquals 'value2', result2.val
        assertEquals 'value1', data.y
        assertEquals 'value2', data.x
    }

    public void testContains() {
        final Dataflows data = new Dataflows()
        assertFalse data.contains('key')
        data.key1 = 'value1'
        assertFalse data.contains('key2')
        assertFalse data.contains('value1')
        assertTrue data.contains('key1')
    }

    public void testUnboundContains() {
        final Dataflows data = new Dataflows()
        final CyclicBarrier barrier = new CyclicBarrier(2)
        Thread.start {
            barrier.await()
            data.key1
        }
        barrier.await(30, TimeUnit.SECONDS)
        Thread.sleep 3000

        assertTrue data.contains('key1')
        data.key1 = 'value1'
        assertTrue data.contains('key1')
        assertFalse data.contains('key2')
        assertFalse data.contains('value1')
        assertTrue data.contains('key1')
    }

    public void testIterator() {
        final Dataflows data = new Dataflows()

        data.x = 0
        data.y = 1
        def log = []
        data.each {entry ->
            log << entry.key
            log << entry.value
        }
        assert 'x' in log
        assert 'y' in log
        assertEquals 2, log.findAll { it in DataflowVariable }.size()

        def log2 = []
        for (entry in data) {
            log2 << entry.key
            log2 << entry.value
        }
        assert log2 == log

        assertEquals 'y', data.find { it.value.val == 1 }.key
        assertEquals 2, data.findAll { it.key.size() == 1 }.size()
        assert data.every { it.key.size() == 1 }
        assert data.any { it.key.size() == 1 }
    }

    public void testInterruption() {
        final def flows = new Dataflows()
        Thread t = Thread.start {
            try {
                flows.x = 1
                flows.y
            } catch (all) {
                flows.e = all
            }
        }

        flows.x
        t.interrupt()
        assert flows.e instanceof InterruptedException

    }
}
