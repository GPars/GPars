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

/**
 * @author Vaclav Pech
 * Date: Mar 8, 2010
 */

public class InternallyParallelDataflowSelectorTest extends GroovyTestCase {

    private PGroup group

    protected void setUp() {
        group = new DefaultPGroup(10)
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

        def op = group.selector(inputs: [a, b, c], outputs: [d, e], maxForks: 2) {x ->
            bindOutput 0, x
            bindOutput 1, 2 * x
        }

        a << 5
        b << 20
        c << 40

        assert [d.val, d.val, d.val].containsAll([5, 20, 40])
        assert [e.val, e.val, e.val].containsAll([10, 40, 80])

        op.terminate()
    }

    public void testDefaultCopySelector() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()

        def op = group.selector(inputs: [a, b, c], outputs: [d, e], maxForks: 2)

        a << 5
        b << 20
        c << 40
        b << 50

        assert [d.val, d.val, d.val, d.val].containsAll([5, 20, 40, 50])
        assert [e.val, e.val, e.val, e.val].containsAll([5, 20, 40, 50])

        op.terminate()
    }

    public void testInvalidMaxForks() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()
        final DataflowVariable d = new DataflowVariable()
        final DataflowQueue e = new DataflowQueue()

        shouldFail(IllegalArgumentException) {
            group.selector(inputs: [a, b, c], outputs: [d, e], maxForks: 0) {x, y, z -> }
        }
        shouldFail(IllegalArgumentException) {
            group.selector(inputs: [a, b, c], outputs: [d, e], maxForks: -1) {x, y, z -> }
        }
    }

    public void testOutputNumber() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def selector1 = group.selector(inputs: [a], outputs: [], maxForks: 2) {v -> terminate()}
        def selector2 = group.selector(inputs: [a], maxForks: 2) {v -> terminate()}
        def selector3 = group.selector(inputs: [a], mistypedOutputs: [d], maxForks: 2) {v -> terminate()}

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
            group.selector(inputs1: [a], outputs: [d], maxForks: 2) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            group.selector(outputs: [d], maxForks: 2) {v -> }
        }
        shouldFail(IllegalArgumentException) {
            group.selector([maxForks: 2]) {v -> }
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

        def op = group.selector(inputs: [stream], outputs: [], maxFork: 3, listeners: [listener]) {
            throw new RuntimeException('test')
        }
        stream << 'value'
        assert a.val instanceof RuntimeException
    }

    public void testExceptionWithDefaultHandler() {
        final DataflowQueue stream = new DataflowQueue()

        def op = group.selector(inputs: [stream], outputs: [], maxFork: 3) {
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
