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
import static groovyx.gpars.dataflow.DataFlow.operator
import static groovyx.gpars.dataflow.DataFlow.prioritySelector
import static groovyx.gpars.dataflow.DataFlow.selector
import static groovyx.gpars.dataflow.DataFlow.splitter

/**
 * @author Vaclav Pech
 * Date: Oct 6, 2009
 */

public class DataFlowOperatorShutdownTest extends GroovyTestCase {

    public void testSingleOperator() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()
        final DataFlowStream e = new DataFlowStream()

        def op = operator(inputs: [a, b, c], outputs: [d, e]) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        a << 10
        b << 20
        c << 30

        assertEquals 60, d.val
        assertEquals 6000, e.val
        a << DataFlowPoisson.instance
        assert d.val == DataFlowPoisson.instance
        assert e.val == DataFlowPoisson.instance
        op.join()
    }

    public void testSplitter() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()
        final DataFlowStream e = new DataFlowStream()

        def op = splitter(a, [d, e])

        a << 10

        assertEquals 10, d.val
        assertEquals 10, e.val
        a << DataFlowPoisson.instance
        assert d.val == DataFlowPoisson.instance
        assert e.val == DataFlowPoisson.instance
        op.join()
    }

    public void testOperatorOnDFV() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable d = new DataFlowVariable()

        def op = operator(inputs: [a], outputs: [d]) {x ->
            bindOutput x
        }

        a << DataFlowPoisson.instance
        assert d.val == DataFlowPoisson.instance
        op.join()
    }

    public void testOperatorNetwork() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()
        final DataFlowStream e = new DataFlowStream()
        final DataFlowStream f = new DataFlowStream()
        final DataFlowStream out = new DataFlowStream()

        def op1 = operator(inputs: [a, b, c], outputs: [d, e]) {x, y, z -> }

        def op2 = operator(inputs: [d], outputs: [f, out]) { }

        def op3 = operator(inputs: [e, f], outputs: [b, out]) {x, y -> }

        a << DataFlowPoisson.instance

        assert out.val == DataFlowPoisson.instance
        assert out.val == DataFlowPoisson.instance
        op1.join()
        op2.join()
        op3.join()
    }

    public void testSelectorNetwork() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowStream d = new DataFlowStream()
        final DataFlowStream e = new DataFlowStream()
        final DataFlowStream f = new DataFlowStream()
        final DataFlowStream out = new DataFlowStream()

        def op1 = selector(inputs: [a, b, c], outputs: [d, e]) {value, index -> }

        def op2 = selector(inputs: [d], outputs: [f, out]) { }

        def op3 = prioritySelector(inputs: [e, f], outputs: [b]) {value, index -> }

        a << DataFlowPoisson.instance

        assert out.val == DataFlowPoisson.instance
        op1.join()
        op2.join()
        op3.join()
    }
}