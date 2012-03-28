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

import groovyx.gpars.DataflowMessagingRunnable
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup

/**
 * @author Vaclav Pech
 * Date: Now 8, 2010
 */

public class DataflowProcessorJavaAPITest extends GroovyTestCase {

    private PGroup group

    protected void setUp() {
        group = new DefaultPGroup(1)
        super.setUp()
    }

    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }

    public void testOperator() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()

        def op = group.operator(inputs: [a, b], outputs: [c], new TestRunnable1())

        Dataflow.task { a << 10 }
        Dataflow.task { b << 20 }

        assert 30 == c.val

        op.terminate()
    }

    public void testSelector() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()

        def op = group.selector(inputs: [a, b], outputs: [c], new TestRunnable2())

        a << 10
        assert 20 == c.val
        b << 20
        assert 40 == c.val

        op.terminate()
    }

    public void testSelectorWithIndex() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()

        def op = group.selector(inputs: [a, b], outputs: [c], new TestRunnable2WithIndex())

        a << 10
        assert [20, 0] == c.val
        b << 20
        assert [40, 1] == c.val

        op.terminate()
    }

    public void testPrioritySelector() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()

        def op = group.prioritySelector(inputs: [a, b], outputs: [c], new TestRunnable2())

        a << 10
        assert 20 == c.val
        b << 20
        assert 40 == c.val

        op.terminate()
    }

    public void testPrioritySelectorWithIndex() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowQueue c = new DataflowQueue()

        def op = group.prioritySelector(inputs: [a, b], outputs: [c], new TestRunnable2WithIndex())

        a << 10
        assert [20, 0] == c.val
        b << 20
        assert [40, 1] == c.val

        op.terminate()
    }
}

class TestRunnable1 extends DataflowMessagingRunnable {

    def TestRunnable1() {
        super(2);
    }

    protected void doRun(Object... arguments) {
        getOwningProcessor().bindOutput(arguments[0] + arguments[1])
    }
}

class TestRunnable2 extends DataflowMessagingRunnable {

    def TestRunnable2() {
        super(1);
    }

    protected void doRun(Object... arguments) {
        getOwningProcessor().bindOutput(2 * arguments[0])
    }
}

class TestRunnable2WithIndex extends DataflowMessagingRunnable {

    def TestRunnable2WithIndex() {
        super(2);
    }

    protected void doRun(Object... arguments) {
        getOwningProcessor().bindOutput([2 * arguments[0], arguments[1]])
    }
}
