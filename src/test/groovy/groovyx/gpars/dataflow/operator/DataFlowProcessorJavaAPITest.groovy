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

import groovyx.gpars.DataFlowMessagingRunnable
import groovyx.gpars.dataflow.DataFlow
import groovyx.gpars.dataflow.DataFlowQueue
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup

/**
 * @author Vaclav Pech
 * Date: Now 8, 2010
 */

public class DataFlowProcessorJavaAPITest extends GroovyTestCase {

    private PGroup group

    protected void setUp() {
        group = new DefaultPGroup(1)
    }

    protected void tearDown() {
        group.shutdown()
    }

    public void testOperator() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowQueue c = new DataFlowQueue()

        def op = group.operator(inputs: [a, b], outputs: [c], new TestRunnable1())

        DataFlow.task { a << 10 }
        DataFlow.task { b << 20 }

        assertEquals 30, c.val

        op.stop()
    }

    public void testSelector() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowQueue c = new DataFlowQueue()

        def op = group.selector(inputs: [a, b], outputs: [c], new TestRunnable2())

        a << 10
        assertEquals 20, c.val
        b << 20
        assertEquals 40, c.val

        op.stop()
    }

    public void testSelectorWithIndex() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowQueue c = new DataFlowQueue()

        def op = group.selector(inputs: [a, b], outputs: [c], new TestRunnable2WithIndex())

        a << 10
        assertEquals([20, 0], c.val)
        b << 20
        assertEquals([40, 1], c.val)

        op.stop()
    }

    public void testPrioritySelector() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowQueue c = new DataFlowQueue()

        def op = group.prioritySelector(inputs: [a, b], outputs: [c], new TestRunnable2())

        a << 10
        assertEquals 20, c.val
        b << 20
        assertEquals 40, c.val

        op.stop()
    }

    public void testPrioritySelectorWithIndex() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowQueue c = new DataFlowQueue()

        def op = group.prioritySelector(inputs: [a, b], outputs: [c], new TestRunnable2WithIndex())

        a << 10
        assertEquals([20, 0], c.val)
        b << 20
        assertEquals([40, 1], c.val)

        op.stop()
    }
}

class TestRunnable1 extends DataFlowMessagingRunnable {

    def TestRunnable1() {
        super(2);
    }

    protected void doRun(Object[] arguments) {
        getOwningProcessor().bindOutput(arguments[0] + arguments[1])
    }
}

class TestRunnable2 extends DataFlowMessagingRunnable {

    def TestRunnable2() {
        super(1);
    }

    protected void doRun(Object[] arguments) {
        getOwningProcessor().bindOutput(2 * arguments[0])
    }
}

class TestRunnable2WithIndex extends DataFlowMessagingRunnable {

    def TestRunnable2WithIndex() {
        super(2);
    }

    protected void doRun(Object[] arguments) {
        getOwningProcessor().bindOutput([2 * arguments[0], arguments[1]])
    }
}