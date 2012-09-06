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

package groovyx.gpars.dataflow.operator

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */

public class DataflowSelectorTest extends GroovyTestCase {

    private PGroup group

    protected void setUp() {
        group = new DefaultPGroup(1)
        super.setUp()
    }

    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }

    public void testSelector() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()

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

        op.terminate()
    }

    public void testSelectorNotResubscribesOnDFVs() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

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

        op.terminate()
    }

    public void testDefaultCopySelector() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()

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

        op.terminate()
    }

    public void testSelectorWithIndex() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()

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

        op.terminate()
    }

    public void testOperatorWithDoubleWaitOnChannel() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
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

        op.terminate()
    }

    public void testStop() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final CyclicBarrier barrier = new CyclicBarrier(2)
        int counter = 0

        def op1 = group.selector(inputs: [a, b], outputs: [c]) {x ->
            counter++
            barrier.await()

        }
        a << 'Delivered'
        sleep 500
        a << 'Never delivered'
        op1.terminate()
        op1.join()
        assert counter == 1
    }

    public void testInterrupt() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        boolean flag = false

        def op1 = group.selector(inputs: [a], outputs: [b]) {v ->
            Thread.currentThread().interrupt()
            flag = true
            bindOutput 'a'
        }
        op1.actor.metaClass.onInterrupt = {}
        assertFalse flag
        a << 'Message'
        assert 'a' == b.val
        assertTrue flag
        op1.terminate()
        op1.join()
    }

    public void testEmptyInputs() {
        final DataflowQueue b = new DataflowQueue()
        boolean flag = false

        shouldFail(IllegalArgumentException) {
            def op1 = group.selector(inputs: [], outputs: [b]) {->
                flag = true
                terminate()
            }
            op1.join()
        }
        assert !flag
    }

    public void testOutputs() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        boolean flag = false

        def op1 = group.selector(inputs: [a], outputs: [b, c]) {
            flag = (output == b) && (outputs[0] == b) && (outputs[1] == c)
            terminate()
        }
        a << null
        op1.join()
        assert flag
        assert (op1.output == b) && (op1.outputs[0] == b) && (op1.outputs[1] == c)
        assert (op1.getOutput() == b) && (op1.getOutputs(0) == b) && (op1.getOutputs(1) == c)
    }

    public void testEmptyOutputs() {
        final DataflowQueue b = new DataflowQueue()
        boolean flag = false

        def op1 = group.selector(inputs: [b], outputs: []) {
            flag = (output == null)
            terminate()
        }
        b << null
        op1.join()
        assert flag
        assert op1.output == null
    }

    public void testInputNumber() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        group.selector(inputs: [a, b], outputs: [d]) {}.terminate()
        group.selector(inputs: [a, b], outputs: [d]) {x ->}.terminate()
        group.selector(inputs: [a, b], outputs: [d]) {x, y ->}.terminate()

        shouldFail(IllegalArgumentException) {
            group.selector(inputs: [a, b, c], outputs: [d]) {x, y, z -> }
        }
        shouldFail(IllegalArgumentException) {
            group.selector(inputs: [], outputs: [d]) { }
        }
        shouldFail(IllegalArgumentException) {
            group.selector(inputs: [a], outputs: [d]) {-> }
        }

        def op1 = group.selector(inputs: [a], outputs: [d]) { }
        op1.terminate()

        op1 = group.selector(inputs: [a], outputs: [d]) {x -> }
        op1.terminate()

        op1 = group.selector(inputs: [a, b], outputs: [d]) {x, y -> }
        op1.terminate()
    }

    public void testOutputNumber() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def selector1 = group.selector(inputs: [a], outputs: []) {v -> terminate()}
        def selector2 = group.selector(inputs: [a]) {v -> terminate()}
        def selector3 = group.selector(inputs: [a], mistypedOutputs: [d]) {v -> terminate()}

        a << 'value'
        a << 'value'
        a << 'value'
        [selector1, selector2, selector3]*.terminate()
        [selector1, selector2, selector3]*.join()
    }

    public void testMissingChannels() {
        final DataflowQueue d = new DataflowQueue()

        shouldFail(IllegalArgumentException) {
            group.selector(outputs: [d]) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            group.selector([:]) {v -> }
        }
    }

    public void testException() {
        final DataflowQueue stream = new DataflowQueue()
        final DataflowVariable a = new DataflowVariable()

        final listener = new DataflowEventAdapter() {
            @Override
            boolean onException(final DataflowProcessor processor, final Throwable e) {
                a << e
                true
            }
        }

        def op = group.selector(inputs: [stream], outputs: [], listeners: [listener]) {
            throw new RuntimeException('test')
        }
        stream << 'value'
        assert a.val instanceof RuntimeException
    }

    public void testExceptionWithDefaultHandler() {
        final DataflowQueue stream = new DataflowQueue()

        def op = group.selector(inputs: [stream], outputs: []) {
            if (it == 'invalidValue') throw new RuntimeException('test')
        }
        op.actor.metaClass.onException = {}
        stream << 'value'
        stream << 'invalidValue'
        op.join()
    }

    public void testGuards() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        final CyclicBarrier barrier = new CyclicBarrier(2)

        def op = group.selector(inputs: [a, b, c], outputs: [d]) {
            if (it == 1) setGuard(0, false)
            if (it == 3) setGuard(2, false)
            if (it == 4) setGuard(0, true)
            if (it == 5) setGuard(2, true)
            bindOutput it
            barrier.await()
        }
        a << 1
        barrier.await()
        a << 2
        sleep 500
        b << 3
        barrier.await()
        c << 4
        b << 5
        barrier.await()
        barrier.await()
        barrier.await()

        assert [d.val, d.val, d.val, d.val, d.val] == [1, 3, 5, 4, 2]
        op.terminate()
        op.join()

    }

    public void testInitialGuards() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

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
        op.terminate()
        op.join()
    }

    public void testSelectorDoesNotConsumeMessagesAfterStop() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()

        group.selector(inputs: [a], outputs: [b]) {
            bindOutput 0, it
            terminate()
        }

        a << 1
        a << 2
        a << 3
        a << 4

        assert 1 == b.val
        assertNull b.getVal(1, TimeUnit.SECONDS)
    }
}
