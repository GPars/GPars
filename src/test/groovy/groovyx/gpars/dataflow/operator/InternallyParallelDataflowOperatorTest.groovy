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

import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup

import java.util.concurrent.CyclicBarrier

/**
 * @author Vaclav Pech
 * Date: Mar 8, 2010
 */

public class InternallyParallelDataflowOperatorTest extends GroovyTestCase {

    private PGroup group

    protected void setUp() {
        group = new DefaultPGroup(10)
        super.setUp()
    }

    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }


    public void testOneForkOperator() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()
        final DataflowVariable d = new DataflowVariable()
        final DataflowQueue e = new DataflowQueue()

        def op = group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: 1) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        Actors.blockingActor { a << 5 }
        Actors.blockingActor { b << 20 }
        Actors.blockingActor { c << 40 }

        assert 65 == d.val
        assert 4000 == e.val

        op.terminate()
    }

    public void testTwoForkOperator() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()
        final DataflowVariable d = new DataflowVariable()
        final DataflowQueue e = new DataflowQueue()

        def op = group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: 2) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        Actors.blockingActor { a << 5 }
        Actors.blockingActor { b << 20 }
        Actors.blockingActor { c << 40 }

        assert 65 == d.val
        assert 4000 == e.val

        op.terminate()
    }

    public void testParallelism() {
        100.times {
            performParallelismTest(5, 4)
            performParallelismTest(5, 5)
            performParallelismTest(3, 5)
        }
    }

    private def performParallelismTest(int poolSize, forks) {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()
        final DefaultPGroup group = new DefaultPGroup(poolSize)

        final int parties = Math.min(poolSize - 1, forks)
        final CyclicBarrier barrier = new CyclicBarrier(parties)

        def op = group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: forks) {x, y, z ->
            barrier.await()
            bindOutput 0, x + y + z
            bindOutput 1, Thread.currentThread().name.hashCode()
        }

        Dataflow.task { a << 5 }
        Dataflow.task { b << 10 }
        Dataflow.task { [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16].each {c << it} }

        def results = (1..16).collect {d.val}
        assert 16 == results.size()
        assert results.containsAll([16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31])

        def threads = (1..16).collect {e.val}
        assert 16 == threads.size()
        assert threads.unique().size() in (parties..[poolSize, forks].max())

        op.terminate()
        op.join()
        group.shutdown()
    }

    public void testInvalidMaxForks() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()
        final DataflowVariable d = new DataflowVariable()
        final DataflowQueue e = new DataflowQueue()

        shouldFail(IllegalArgumentException) {
            group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: 0) {x, y, z -> }
        }
        shouldFail(IllegalArgumentException) {
            group.operator(inputs: [a, b, c], outputs: [d, e], maxForks: -1) {x, y, z -> }
        }
    }

    public void testOutputNumber() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def selector1 = group.operator(inputs: [a], outputs: [], maxForks: 2) {v -> terminate()}
        def selector2 = group.operator(inputs: [a], maxForks: 2) {v -> terminate()}
        def selector3 = group.operator(inputs: [a], mistypedOutputs: [d], maxForks: 2) {v -> terminate()}

        a << 'value'
        a << 'value'
        a << 'value'
        [selector1, selector2, selector3]*.terminate()
        [selector1, selector2, selector3]*.join()
    }

    public void testMissingChannels() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        shouldFail(IllegalArgumentException) {
            group.operator(inputs1: [a], outputs: [d], maxForks: 2) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            group.operator(outputs: [d], maxForks: 2) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            group.operator([maxForks: 2]) {v -> }
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

        def op = group.operator(inputs: [stream], outputs: [], maxFork: 3, listeners: [listener]) {
            throw new RuntimeException('test')
        }
        stream << 'value'
        assert a.val instanceof RuntimeException
    }

    public void testExceptionWithDefaultHandler() {
        final DataflowQueue stream = new DataflowQueue()

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
