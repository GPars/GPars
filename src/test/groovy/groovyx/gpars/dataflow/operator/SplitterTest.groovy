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

import groovyx.gpars.dataflow.DataFlowStream
import groovyx.gpars.dataflow.DataFlowVariable
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
    }

    protected void tearDown() {
        group.shutdown()
    }

    public void testSplit() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

        def op = group.splitter(a, [b, c, d])

        a << 1
        a << 2
        a << 3
        a << 4
        a << 5

        assert [b.val, b.val, b.val, b.val, b.val] == [1, 2, 3, 4, 5]
        assert [c.val, c.val, c.val, c.val, c.val] == [1, 2, 3, 4, 5]
        assert [d.val, d.val, d.val, d.val, d.val] == [1, 2, 3, 4, 5]

        op.stop()
    }

    public void testSplitWithMultipleForks() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

        def op = group.splitter(a, [b, c, d], 5)

        a << 1
        a << 2
        a << 3
        a << 4
        a << 5

        assert [b.val, b.val, b.val, b.val, b.val].containsAll([1, 2, 3, 4, 5])
        assert [c.val, c.val, c.val, c.val, c.val].containsAll([1, 2, 3, 4, 5])
        assert [d.val, d.val, d.val, d.val, d.val].containsAll([1, 2, 3, 4, 5])

        op.stop()
    }

    public void testStop() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()

        def op = group.splitter(a, [b, c, d])

        a << 'Delivered'
        op.stop()
        a << 'Never delivered'
        op.join()
        assert b.val == 'Delivered'
        assert c.val == 'Delivered'
        assert d.val == 'Delivered'
        assert d.getVal(10, TimeUnit.MILLISECONDS) == null
    }


    public void testEmptyInputsOrOutputs() {
        shouldFail(IllegalArgumentException) {
            group.splitter(null, [new DataFlowVariable()])
        }
        shouldFail(IllegalArgumentException) {
            group.splitter(new DataFlowVariable(), [])
        }
        shouldFail(IllegalArgumentException) {
            group.splitter(new DataFlowVariable(), null)
        }
    }
}