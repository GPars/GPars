// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.dataflow.operator

import groovyx.gpars.dataflow.DataFlowQueue
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */

public class DataFlowSelectorTest extends GroovyTestCase {

    private PGroup group

    protected void setUp() {
        group = new DefaultPGroup(1)
    }

    protected void tearDown() {
        group.shutdown()
    }

    public void testSelector() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()
        final DataFlowQueue e = new DataFlowQueue()

        def op = group.selector(inputs: [a, b, c], outputs: [d, e]) {x ->
            bindOutput 0, x
            bindOutput 1, 2 * x
        }

        a << 5
        sleep 500
        b << 20
        sleep 500
        c << 40

        assert [d.val, d.val, d.val] == [5, 20, 40]
        assert [e.val, e.val, e.val] == [10, 40, 80]

        op.stop()
    }

    public void testSelectorNotResubscribesOnDFVs() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()

        def op = group.selector(inputs: [a, b, c], outputs: [d]) {x ->
            bindOutput 0, x
        }

        a << 5
        sleep 500
        b << 20
        sleep 500
        c << 40
        sleep 500
        c << 50
        sleep 1000
        c << 60

        assert [d.val, d.val, d.val, d.val, d.val] == [5, 20, 40, 50, 60]

        op.stop()
    }

    public void testDefaultCopySelector() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()
        final DataFlowQueue e = new DataFlowQueue()

        def op = group.selector(inputs: [a, b, c], outputs: [d, e])

        a << 5
        sleep 500
        b << 20
        sleep 500
        c << 40
        sleep 500
        b << 50

        assert [d.val, d.val, d.val, d.val] == [5, 20, 40, 50]
        assert [e.val, e.val, e.val, e.val] == [5, 20, 40, 50]

        op.stop()
    }

    public void testSelectorWithIndex() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()
        final DataFlowQueue e = new DataFlowQueue()

        def op = group.selector(inputs: [a, b, c], outputs: [d, e]) {x, index ->
            bindOutput 0, x
            bindOutput 1, index
        }

        a << 5
        sleep 500
        b << 20
        sleep 500
        c << 40
        sleep 500
        b << 50
        sleep 500
        c << 60

        assert [d.val, d.val, d.val, d.val, d.val] == [5, 20, 40, 50, 60]
        assert [e.val, e.val, e.val, e.val, e.val] == [0, 1, 2, 1, 2]

        op.stop()
    }

    public void testOperatorWithDoubleWaitOnChannel() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final CyclicBarrier barrier = new CyclicBarrier(2)

        def op = group.selector(inputs: [a, a], outputs: [b]) {x ->
            bindOutput 0, x
            barrier.await()
        }

        a << 1
        barrier.await()
        a << 2
        barrier.await()
        a << 3
        barrier.await()
        a << 4
        barrier.await()

        assert [b.val, b.val, b.val, b.val] == [1, 2, 3, 4]

        op.stop()
    }

    public void testStop() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue c = new DataFlowQueue()
        final CyclicBarrier barrier = new CyclicBarrier(2)
        volatile int counter = 0

        def op1 = group.selector(inputs: [a, b], outputs: [c]) {x ->
            counter++
            barrier.await()

        }
        a << 'Delivered'
        sleep 500
        a << 'Never delivered'
        op1.stop()
        barrier.await()
        op1.join()
        assert counter == 1
    }

    public void testInterrupt() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        volatile boolean flag = false

        def op1 = group.selector(inputs: [a], outputs: [b]) {v ->
            Thread.currentThread().interrupt()
            flag = true
            bindOutput 'a'
        }
        op1.actor.metaClass.onInterrupt = {}
        assertFalse flag
        a << 'Message'
        assertEquals 'a', b.val
        assertTrue flag
        op1.stop()
        op1.join()
    }

    public void testEmptyInputs() {
        final DataFlowQueue b = new DataFlowQueue()
        volatile boolean flag = false

        shouldFail(IllegalArgumentException) {
            def op1 = group.selector(inputs: [], outputs: [b]) {->
                flag = true
                stop()
            }
            op1.join()
        }
        assert !flag
    }

    public void testOutputs() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue c = new DataFlowQueue()
        volatile boolean flag = false

        def op1 = group.selector(inputs: [a], outputs: [b, c]) {
            flag = (output == b) && (outputs[0] == b) && (outputs[1] == c)
            stop()
        }
        a << null
        op1.join()
        assert flag
        assert (op1.output == b) && (op1.outputs[0] == b) && (op1.outputs[1] == c)
        assert (op1.getOutput() == b) && (op1.getOutputs(0) == b) && (op1.getOutputs(1) == c)
    }

    public void testEmptyOutputs() {
        final DataFlowQueue b = new DataFlowQueue()
        volatile boolean flag = false

        def op1 = group.selector(inputs: [b], outputs: []) {
            flag = (output == null)
            stop()
        }
        b << null
        op1.join()
        assert flag
        assert op1.output == null
    }

    public void testInputNumber() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()

        group.selector(inputs: [a, b], outputs: [d]) {}.stop()
        group.selector(inputs: [a, b], outputs: [d]) {x ->}.stop()
        group.selector(inputs: [a, b], outputs: [d]) {x, y ->}.stop()

        shouldFail(IllegalArgumentException) {
            def op1 = group.selector(inputs: [a, b, c], outputs: [d]) {x, y, z -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = group.selector(inputs: [], outputs: [d]) { }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = group.selector(inputs: [a], outputs: [d]) {-> }
        }

        def op1 = group.selector(inputs: [a], outputs: [d]) { }
        op1.stop()

        op1 = group.selector(inputs: [a], outputs: [d]) {x -> }
        op1.stop()

        op1 = group.selector(inputs: [a, b], outputs: [d]) {x, y -> }
        op1.stop()
    }

    public void testOutputNumber() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()

        def selector1 = group.selector(inputs: [a], outputs: []) {v -> stop()}
        def selector2 = group.selector(inputs: [a]) {v -> stop()}
        def selector3 = group.selector(inputs: [a], mistypedOutputs: [d]) {v -> stop()}

        a << 'value'
        a << 'value'
        a << 'value'
        [selector1, selector2, selector3]*.stop()
        [selector1, selector2, selector3]*.join()
    }

    public void testMissingChannels() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()

        shouldFail(IllegalArgumentException) {
            def op1 = group.selector(outputs: [d]) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = group.selector([:]) {v -> }
        }
    }

    public void testException() {
        final DataFlowQueue stream = new DataFlowQueue()
        final DataFlowVariable a = new DataFlowVariable()

        def op = group.selector(inputs: [stream], outputs: []) {
            throw new RuntimeException('test')
        }
        op.metaClass.reportError = {Throwable e ->
            a << e
            stop()
        }
        stream << 'value'
        assert a.val instanceof RuntimeException
    }

    public void testExceptionWithDefaultHandler() {
        final DataFlowQueue stream = new DataFlowQueue()
        final DataFlowVariable a = new DataFlowVariable()

        def op = group.selector(inputs: [stream], outputs: []) {
            if (it == 'invalidValue') throw new RuntimeException('test')
        }
        op.actor.metaClass.onException = {}
        stream << 'value'
        stream << 'invalidValue'
        op.join()
    }

    public void testGuards() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()

        def op = group.selector(inputs: [a, b, c], outputs: [d]) {
            if (it == 1) setGuard(0, false)
            if (it == 3) setGuard(2, false)
            if (it == 4) setGuard(0, true)
            if (it == 5) setGuard(2, true)
            bindOutput it
        }
        a << 1
        sleep 500
        a << 2
        sleep 500
        b << 3
        sleep 500
        c << 4
        sleep 500
        b << 5

        assert [d.val, d.val, d.val, d.val, d.val] == [1, 3, 5, 4, 2]
        op.stop()
        op.join()

    }

    public void testInitialGuards() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()

        def op = group.selector(inputs: [a, b, c], outputs: [d], guards: [false, true, true]) {
            if (it == 3) setGuards([true, false, false])
            if (it == 2) setGuard(2, true)
            bindOutput it
        }
        a << 1
        sleep 500
        b << 3
        sleep 500
        c << 4
        sleep 500
        a << 2

        assert [d.val, d.val, d.val, d.val] == [3, 1, 2, 4]
        op.stop()
        op.join()
    }

    public void testSelectorDoesNotConsumeMessagesAfterStop() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()

        def op = group.selector(inputs: [a], outputs: [b]) {
            bindOutput 0, it
            stop()
        }

        a << 1
        a << 2
        a << 3
        a << 4

        assertEquals 1, b.val
        assertNull b.getVal(1, TimeUnit.SECONDS)
    }
}