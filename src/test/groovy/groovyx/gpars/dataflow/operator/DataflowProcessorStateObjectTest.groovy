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
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup
/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */

public class DataflowProcessorStateObjectTest extends GroovyTestCase {

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
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()

        def op = group.operator(inputs: [a, b], outputs: [c], stateObject: [counter: 0]) {x, y ->
            stateObject.counter += x+y
            bindOutput stateObject.counter
        }

        a << 10
        b << 20
        assert 30 == c.val
        a << 1
        b << 2
        assert 33 == c.val

        op.terminate()
    }

    public void testSelector() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()

        def op = group.selector(inputs: [a, b], outputs: [c], stateObject: [0]) {x->
            stateObject[0] += x
            bindOutput stateObject
        }

        a << 10
        assert [10] == c.val
        b << 20
        assert [30] == c.val
        a << 1
        assert [31] == c.val
        b << 2
        assert [33] == c.val

        op.terminate()
    }

    public void testNullStateObject() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()

        def op = group.selector(inputs: [a, b], outputs: [c], stateObject: null) {x->
            bindOutput stateObject
        }

        a << 10
        assert null == c.val

        op.terminate()
    }

    public void testUndefinedStateObject() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()

        def op = group.selector(inputs: [a, b], outputs: [c]) {x->
            bindOutput stateObject
        }

        a << 10
        assert null == c.val

        op.terminate()
    }
}
