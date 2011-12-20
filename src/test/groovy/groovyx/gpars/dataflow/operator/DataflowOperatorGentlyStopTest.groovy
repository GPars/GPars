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
import java.util.concurrent.TimeUnit

/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */

public class DataflowOperatorGentlyStopTest extends GroovyTestCase {

    private PGroup group

    protected void setUp() {
        group = new DefaultPGroup(1)
        super.setUp()
    }

    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }

    public void testOperatorStop() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = group.operator(inputs: [a, b, c], outputs: [d]) {x, y, z ->
            bindOutput 0, x + y + z
        }

        a << 5
        b << 20
        c << 40

        assert 65 == d.val
        op.terminateAfterNextRun()

        a << 10
        b << 20
        c << 40

        assert 70 == d.val
        op.join()
    }

    public void testOperatorStopWhileProcessing() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = group.operator(inputs: [a, b, c], outputs: [d]) {x, y, z ->
            bindOutput 0, x + y + z
        }

        a << 10
        op.terminateAfterNextRun()
        b << 20
        c << 40

        assert 70 == d.val
        op.join()
    }

    public void testSelectorStop() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = group.selector(inputs: [a, b, c], outputs: [d]) {x ->
            bindOutput 0, x
        }

        a << 5
        b << 20
        c << 40

        assert [5, 20, 40] as Set == [d.val, d.val, d.val] as Set
        op.terminateAfterNextRun()

        a << 10
        b << 20
        c << 40

        assert d.val in [10, 20, 40]
        op.join()
        assert d.getVal(1, TimeUnit.SECONDS) == null
    }
}
