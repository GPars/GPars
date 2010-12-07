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

import groovyx.gpars.dataflow.DataFlow
import groovyx.gpars.dataflow.DataFlowQueue
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup
import java.util.concurrent.CyclicBarrier

/**
 * @author Vaclav Pech
 * Date: Mar 8, 2010
 */

public class InternallyParallelDataFlowOperatorTest extends GroovyTestCase {

    private PGroup group

    protected void setUp() {
        group = new DefaultPGroup(10)
    }

    protected void tearDown() {
        group.shutdown()
    }


    public void testOneForkOperator() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowVariable d = new DataFlowVariable()
        final DataFlowQueue e = new DataFlowQueue()

        def op = group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: 1) {x, y, z ->
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

    public void testTwoForkOperator() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowVariable d = new DataFlowVariable()
        final DataFlowQueue e = new DataFlowQueue()

        def op = group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: 2) {x, y, z ->
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

    public void testParallelism() {
        100.times {
            performParallelismTest(5, 4)
            performParallelismTest(5, 5)
            performParallelismTest(3, 5)
        }
    }

    private def performParallelismTest(int poolSize, forks) {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()
        final DataFlowQueue e = new DataFlowQueue()
        final DefaultPGroup group = new DefaultPGroup(poolSize)

        final int parties = Math.min(poolSize - 1, forks)
        final CyclicBarrier barrier = new CyclicBarrier(parties)

        def op = group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: forks) {x, y, z ->
            barrier.await()
            bindOutput 0, x + y + z
            bindOutput 1, Thread.currentThread().name.hashCode()
        }

        DataFlow.task { a << 5 }
        DataFlow.task { b << 10 }
        DataFlow.task { [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16].each {c << it} }

        def results = (1..16).collect {d.val}
        assertEquals 16, results.size()
        assert results.containsAll([16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31])

        def threads = (1..16).collect {e.val}
        assertEquals 16, threads.size()
        assert threads.unique().size() in (parties..[poolSize, forks].max())

        op.stop()
        op.join()
        group.shutdown()
    }

    public void testInvalidMaxForks() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowQueue c = new DataFlowQueue()
        final DataFlowVariable d = new DataFlowVariable()
        final DataFlowQueue e = new DataFlowQueue()

        shouldFail(IllegalArgumentException) {
            def op = group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: 0) {x, y, z -> }
        }
        shouldFail(IllegalArgumentException) {
            def op = group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: -1) {x, y, z -> }
        }
    }

    public void testOutputNumber() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()

        def selector1 = group.operator(inputs: [a], outputs: [], maxForks: 2) {v -> stop()}
        def selector2 = group.operator(inputs: [a], maxForks: 2) {v -> stop()}
        def selector3 = group.operator(inputs: [a], mistypedOutputs: [d], maxForks: 2) {v -> stop()}

        a << 'value'
        a << 'value'
        a << 'value'
        [selector1, selector2, selector3]*.stop()
        [selector1, selector2, selector3]*.join()
    }

    public void testMissingChannels() {
        final DataFlowQueue a = new DataFlowQueue()
        final DataFlowQueue b = new DataFlowQueue()
        final DataFlowQueue d = new DataFlowQueue()

        shouldFail(IllegalArgumentException) {
            def op1 = group.operator(inputs1: [a], outputs: [d], maxForks: 2) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = group.operator(outputs: [d], maxForks: 2) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            def op1 = group.operator([maxForks: 2]) {v -> }
        }
    }

    public void testException() {
        final DataFlowQueue stream = new DataFlowQueue()
        final DataFlowVariable a = new DataFlowVariable()

        def op = group.operator(inputs: [stream], outputs: [], maxFork: 3) {
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

        def op = group.operator(inputs: [stream], outputs: [], maxFork: 3) {
            if (it == 'invalidValue') throw new RuntimeException('test')
        }
        stream << 'value'
        stream << 'value'
        stream << 'value'
        stream << 'value'
        stream << 'value'
        stream << 'invalidValue'
        op.join()
    }
}