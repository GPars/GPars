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
import groovyx.gpars.dataflow.DataFlowStream
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.group.DefaultPGroup
import static groovyx.gpars.dataflow.DataFlow.operator

/**
 * @author Vaclav Pech
 * Date: Mar 8, 2010
 */

public class InternallyParallelDataFlowOperatorTest extends GroovyTestCase {

    public void testOneForkOperator() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowVariable d = new DataFlowVariable()
        final DataFlowStream e = new DataFlowStream()

        def op = operator(inputs: [a, b, c], outputs: [d, e], maxForks: 1) {x, y, z ->
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
        final DataFlowStream c = new DataFlowStream()
        final DataFlowVariable d = new DataFlowVariable()
        final DataFlowStream e = new DataFlowStream()

        def op = operator(inputs: [a, b, c], outputs: [d, e], maxForks: 2) {x, y, z ->
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
        performParallelismTest(5, 3)
        performParallelismTest(5, 5)
        performParallelismTest(3, 5)
    }

    private def performParallelismTest(int poolSize, forks) {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()
        final DataFlowStream e = new DataFlowStream()
        final DefaultPGroup group = new DefaultPGroup(poolSize)

        def op = group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: forks) {x, y, z ->
            sleep 1000
            bindOutput 0, x + y + z
            bindOutput 1, Thread.currentThread().name
        }

        DataFlow.start { a << 5 }
        DataFlow.start { b << 10 }
        DataFlow.start { [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].each {c << it} }

        def results = (1..10).collect {d.val}
        assertEquals 10, results.size()
        assert results.containsAll([16, 17, 18, 19, 20, 21, 22, 23, 24, 25])

        def threads = (1..10).collect {e.val}
        assertEquals 10, threads.size()
        assert threads.unique().size() in (poolSize..forks)

        op.stop()
        group.shutdown()
    }

    public void testInvalidMaxForks() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowVariable d = new DataFlowVariable()
        final DataFlowStream e = new DataFlowStream()

        shouldFail(IllegalArgumentException) {
            def op = operator(inputs: [a, b, c], outputs: [d, e], maxForks: 0) {x, y, z -> }
        }
        shouldFail(IllegalArgumentException) {
            def op = operator(inputs: [a, b, c], outputs: [d, e], maxForks: -1) {x, y, z -> }
        }
    }

    public void testOutputNumber() {
        final DefaultPGroup group = new DefaultPGroup(1)
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

        group.operator(inputs: [a], outputs: [], maxForks: 2) {v -> stop()}
        group.operator(inputs: [a], maxForks: 2) {v -> stop()}
        group.operator(inputs: [a], mistypedOutputs: [d], maxForks: 2) {v -> stop()}

        a << 'value'
        a << 'value'
        a << 'value'
    }

    public void testMissingChannels() {
        final DefaultPGroup group = new DefaultPGroup(1)
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

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
        final DataFlowStream stream = new DataFlowStream()
        final DataFlowVariable a = new DataFlowVariable()

        def op = operator(inputs: [stream], outputs: [], maxFork: 3) {
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
        final DataFlowStream stream = new DataFlowStream()
        final DataFlowVariable a = new DataFlowVariable()

        def op = operator(inputs: [stream], outputs: [], maxFork: 3) {
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