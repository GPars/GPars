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
import java.util.concurrent.TimeUnit

/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */

public class SplitterTest extends GroovyTestCase {

    private PGroup group

    protected void setUp() {
        group = new DefaultPGroup(1)
        super.setUp()
    }

    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }

    public void testSplit() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = group.splitter(a, [b, c, d])

        a << 1
        a << 2
        a << 3
        a << 4
        a << 5

        assert [b.val, b.val, b.val, b.val, b.val] == [1, 2, 3, 4, 5]
        assert [c.val, c.val, c.val, c.val, c.val] == [1, 2, 3, 4, 5]
        assert [d.val, d.val, d.val, d.val, d.val] == [1, 2, 3, 4, 5]

        op.terminate()
    }

    public void testSplitWithMultipleForks() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = group.splitter(a, [b, c, d], 5)

        a << 1
        a << 2
        a << 3
        a << 4
        a << 5

        assert [b.val, b.val, b.val, b.val, b.val].containsAll([1, 2, 3, 4, 5])
        assert [c.val, c.val, c.val, c.val, c.val].containsAll([1, 2, 3, 4, 5])
        assert [d.val, d.val, d.val, d.val, d.val].containsAll([1, 2, 3, 4, 5])

        op.terminate()
    }

    public void testStop() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = group.splitter(a, [b, c, d])

        a << 'Delivered'
        assert b.val == 'Delivered'
        op.terminate()
        a << 'Never delivered'
        op.join()
        assert c.val == 'Delivered'
        assert d.val == 'Delivered'
        assert d.getVal(10, TimeUnit.MILLISECONDS) == null
    }


    public void testEmptyInputsOrOutputs() {
        shouldFail(IllegalArgumentException) {
            group.splitter(null, [new DataflowVariable()])
        }
        shouldFail(IllegalArgumentException) {
            group.splitter(new DataflowVariable(), [])
        }
        shouldFail(IllegalArgumentException) {
            group.splitter(new DataflowVariable(), null)
        }
    }
}
