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
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

public class SyncDataflowVariableTest extends GroovyTestCase {

    public void testWriterBlocking() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(1)
        final AtomicBoolean reached = new AtomicBoolean(false)

        def t = Thread.start {
            variable << 10
            reached.set(true);
        }
        sleep 1000
        assert !reached.get()
        assert 10 == variable.val
        t.join()
        assert 10 == variable.val
        assert 10 == variable.val
        assert 10 == variable.val

        assert reached.get()
    }

    public void testReaderBlocking() {
        final SyncDataflowVariable<Integer> variable = new SyncDataflowVariable<Integer>(2)
        final AtomicBoolean writerReached = new AtomicBoolean(false)
        final AtomicInteger readerReached = new AtomicInteger(0)

        def t1 = Thread.start {
            variable << 10
            writerReached.set(true)
        }
        def t2 = Thread.start {
            readerReached.set(variable.val)
        }
        sleep 1000
        assert !writerReached.get()
        assert readerReached.get() == 0
        assert 10 == variable.val
        [t1, t2]*.join()
        assert writerReached.get()
        assert readerReached.get() == 10
    }

    public void testGet() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(1)
        Thread.start {variable << 10}
        assert 10 == variable.get()
    }

    public void testMultiGet() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(3)
        final dataflows = new Dataflows()
        Thread.start {variable << 10}
        Thread.start {dataflows.a = variable.get()}
        Thread.start {dataflows.b = variable.get()}
        Thread.start {dataflows.c = variable.get(10, java.util.concurrent.TimeUnit.SECONDS)}
        assert 10 == dataflows.a
        assert 10 == dataflows.b
        assert 10 == dataflows.c
    }

    public void testTimeoutGet() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(1)
        shouldFail(TimeoutException) {
            variable.get(1, TimeUnit.SECONDS)
        }
        Thread.start {variable << 10}
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    public void testTimeoutGetWithMultipleParties() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(2)
        Thread.start {variable << 10}

        assert null == variable.getVal(1, TimeUnit.MILLISECONDS)
        assert null == variable.getVal(1, TimeUnit.MILLISECONDS)
        assert null == variable.getVal(1, TimeUnit.MILLISECONDS)

        shouldFail(TimeoutException) {
            variable.get(1, TimeUnit.SECONDS)
        }
        Thread.start {
            variable.get(10, TimeUnit.SECONDS)
        }
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    public void testAsyncRead() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(2)
        def result1 = new DataflowVariable()
        def group = new NonDaemonPGroup(1)
        def actor = group.actor {
            react {
                result1 << it
            }
        }

        Thread.start {
            variable.getValAsync(actor)
        }

        def result2 = new DataflowVariable()
        Thread.start {
            variable.whenBound({result2 << it})
        }

        Thread.start {variable << 10}

        assert 10 == result1.val
        assert 10 == result2.val
        group.shutdown()
    }

    public void testWriterBlockingWithResizing() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(2)
        final AtomicBoolean reached = new AtomicBoolean(false)

        variable.decrementParties()

        def t = Thread.start {
            variable << 10
            reached.set(true)
        }
        sleep 1000
        assert !reached.get()
        assert 10 == variable.val
        t.join()
        assert 10 == variable.val
        assert 10 == variable.val
        assert 10 == variable.val

        assert reached.get()
    }

    public void testReaderBlockingWithResizing() {
        final SyncDataflowVariable<Integer> variable = new SyncDataflowVariable<Integer>(1)
        final AtomicBoolean writerReached = new AtomicBoolean(false)
        final AtomicInteger readerReached = new AtomicInteger(0)

        def t1 = Thread.start {
            variable << 10
            writerReached.set(true)
        }

        sleep 1000
        variable.incrementParties()

        def t2 = Thread.start {
            readerReached.set(variable.val)
        }
        sleep 1000
        assert !writerReached.get()
        assert readerReached.get() == 0
        assert 10 == variable.val
        [t1, t2]*.join()
        assert writerReached.get()
        assert readerReached.get() == 10
    }

    public void testWriterBlockingWithDecrease() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(3)
        final AtomicBoolean reached = new AtomicBoolean(false)

        variable.decrementParties()

        def t = Thread.start {
            variable << 10
            reached.set(true)
        }
        variable.decrementParties()

        sleep 1000
        assert !reached.get()
        assert 10 == variable.val
        t.join()
        assert 10 == variable.val
        assert 10 == variable.val
        assert 10 == variable.val

        assert reached.get()
    }

    public void testReaderBlockingWithDecrease() {
        final SyncDataflowVariable<Integer> variable = new SyncDataflowVariable<Integer>(3)
        final AtomicBoolean writerReached = new AtomicBoolean(false)
        final AtomicInteger readerReached = new AtomicInteger(0)

        def t1 = Thread.start {
            variable << 10
            writerReached.set(true)
        }

        sleep 1000
        variable.decrementParties()

        def t2 = Thread.start {
            readerReached.set(variable.val)
        }
        sleep 1000
        assert !writerReached.get()
        assert readerReached.get() == 0
        assert 10 == variable.val
        [t1, t2]*.join()
        assert writerReached.get()
        assert readerReached.get() == 10
    }

    public void testDecreaseBelowZero() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(3)
        variable.decrementParties()
        variable.decrementParties()
        variable.decrementParties()
        variable.decrementParties()
    }

    public void testAwaitingParties() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(2)
        assert variable.awaitingParties()
        assert variable.shouldThrowTimeout()

        def t = Thread.start {
            variable << 10
        }
        def t2 = Thread.start {
            variable.val
        }

        sleep 1000
        assert variable.awaitingParties()
        assert variable.shouldThrowTimeout()
        assert 10 == variable.val
        assert !variable.awaitingParties()
        t.join()
        assert !variable.awaitingParties()
        assert !variable.shouldThrowTimeout()
    }
}
