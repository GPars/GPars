//  GPars (formerly GParallelizer)
//
//  Copyright � 2008-9  The original author or authors
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

package groovyx.gpars.dataflow.operator

import groovyx.gpars.actor.PooledActorGroup
import groovyx.gpars.dataflow.DataFlow
import groovyx.gpars.dataflow.DataFlowStream
import groovyx.gpars.dataflow.DataFlowVariable
import static groovyx.gpars.dataflow.DataFlow.operator

/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */

public class DataFlowOperatorTest extends GroovyTestCase {

    public void testOperator() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowVariable d = new DataFlowVariable()
        final DataFlowStream e = new DataFlowStream()

        def op = operator(inputs: [a, b, c], outputs: [d, e]) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        DataFlow.start { a << 5 }
        DataFlow.start { b << 20 }
        DataFlow.start { c << 40 }

        assertEquals 65, d.val
        assertEquals 4000, e.val

        op.stop()
    }

    public void testOperatorWithDoubleWaitOnChannel() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()

        def op = operator(inputs: [a, a], outputs: [b]) {x, y ->
            bindOutput 0, x + y
        }

        a << 1
        a << 2
        a << 3
        a << 4

        assertEquals 3, b.val
        assertEquals 7, b.val

        op.stop()
    }

    public void testNonCommutativeOperator() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()

        def op = operator(inputs: [a, b], outputs: [c]) {x, y ->
            bindOutput 0, 2 * x + y
        }

        DataFlow.start { a << 5 }
        DataFlow.start { b << 20 }

        assertEquals 30, c.val

        op.stop()
    }

    public void testCombinedOperators() {
        final PooledActorGroup group = new PooledActorGroup(1)
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        a << 1
        a << 2
        a << 3
        b << 4
        b << 5
        b << 6
        b << 7

        final DataFlowStream x = new DataFlowStream()
        def op1 = operator(inputs: [a], outputs: [x], group) {v ->
            bindOutput v * v
        }

        final DataFlowStream y = new DataFlowStream()
        def op2 = operator(inputs: [b], outputs: [y], group) {v ->
            bindOutput v * v
        }

        def op3 = operator(inputs: [x, y], outputs: [c], group) {v1, v2 ->
            bindOutput v1 + v2
        }

        assertEquals 17, c.val
        assertEquals 29, c.val
        assertEquals 45, c.val
        [op1, op2, op3]*.stop()
    }

    public void testStop() {
        final PooledActorGroup group = new PooledActorGroup(1)
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        volatile boolean flag = false

        def op1 = operator(inputs: [a, b], outputs: [c], group) {x, y ->
            flag = true
        }
        a << 'Never delivered'
        op1.stop()
        op1.join()
        assertFalse flag
    }

    public void testInterrupt() {
        final PooledActorGroup group = new PooledActorGroup(1)
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        volatile boolean flag = false

        def op1 = operator(inputs: [a], outputs: [b], group) {v ->
            Thread.currentThread().interrupt()
            flag = true
            bindOutput 'a'
        }
        assertFalse flag
        a << 'Message'
        assertEquals 'a', b.val
        assertTrue flag
        op1.stop()
        op1.join()
    }

    public void testEmptyInputs() {
        final PooledActorGroup group = new PooledActorGroup(1)
        final DataFlowStream b = new DataFlowStream()
        volatile boolean flag = false

        def op1 = operator(inputs: [], outputs: [b], group) {->
            flag = true
            stop()
        }
        op1.join()
        assert flag
    }

    public void testOutputs() {
        final PooledActorGroup group = new PooledActorGroup(1)
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        volatile boolean flag = false

        def op1 = operator(inputs: [a], outputs: [b, c], group) {
            println delegate
            flag = (output==b) && (outputs[0]==b) && (outputs[1]==c)
            stop()
        }
        a << null
        op1.join()
        assert flag
        assert (op1.output==b) && (op1.outputs[0]==b) && (op1.outputs[1]==c)
        assert (op1.getOutput()==b) && (op1.getOutputs(0)==b) && (op1.getOutputs(1)==c)
    }

    public void testEmptyOutputs() {
        final PooledActorGroup group = new PooledActorGroup(1)
        final DataFlowStream b = new DataFlowStream()
        volatile boolean flag = false

        def op1 = operator(inputs: [b], outputs: [], group) {
            flag = (output==null)
            stop()
        }
        b << null
        op1.join()
        assert flag
        assert op1.output == null
    }

    public void testInputNumber() {
        final PooledActorGroup group = new PooledActorGroup(1)
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

        shouldFail(IllegalArgumentException) {
            def op1 = operator(inputs: [a, b, c], outputs: [d], group) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = operator(inputs: [a, b], outputs: [d], group) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = operator(inputs: [a, b], outputs: [d], group) {x, y, z -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = operator(inputs: [a, b], outputs: [d], group) {}
        }
        shouldFail(IllegalArgumentException) {
            def op1 = operator(inputs: [a], outputs: [d], group) {x, y -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = operator(inputs: [], outputs: [d], group) { }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = operator(inputs: [a], outputs: [d], group) {-> }
        }

        def op1 = operator(inputs: [a], outputs: [d], group) { }
        op1.stop()

        op1 = operator(inputs: [a], outputs: [d], group) {x -> }
        op1.stop()

        op1 = operator(inputs: [a, b], outputs: [d], group) {x, y -> }
        op1.stop()
    }

    public void testMissingChannels() {
        final PooledActorGroup group = new PooledActorGroup(1)
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

        shouldFail(IllegalArgumentException) {
            def op1 = operator(inputs1: [a], outputs: [d], group) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = operator(inputs: [a], outputs2: [d], group) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = operator(outputs: [d], group) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = operator(inputs: [d], group) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = operator([:], group) {v -> }
        }
    }
}