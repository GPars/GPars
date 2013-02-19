// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

import static groovyx.gpars.dataflow.Dataflow.*

/**
 * @author Vaclav Pech
 * Date: Oct 6, 2009
 */

public class DataflowOperatorShutdownTest extends GroovyTestCase {

    public void testSingleOperator() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()

        def op = operator(inputs: [a, b, c], outputs: [d, e]) { x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        a << 10
        b << 20
        c << 30

        assert 60 == d.val
        assert 6000 == e.val
        a << PoisonPill.instance
        assert d.val == PoisonPill.instance
        assert e.val == PoisonPill.instance
        op.join()
    }

    public void testSingleOperatorWithEmptyOutput() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = operator(inputs: [a, b, c], outputs: []) { x, y, z ->
            d << x + y + z
        }

        a << 10
        b << 20
        c << 30

        assert 60 == d.val
        a << PoisonPill.instance
        op.join()
        assert !d.isBound()
    }

    public void testSingleOperatorWithNoOutput() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = operator(inputs: [a, b, c], listeners: [new DataflowEventAdapter() {
            @Override
            boolean onException(final DataflowProcessor processor, final Throwable e) {
                d << e
                return super.onException(processor, e)
            }
        }]) { x, y, z ->
            d << x + y + z
        }

        a << 10
        b << 20
        c << 30

        assert 60 == d.val
        a << PoisonPill.instance
        op.join()
        assert !d.isBound()
    }

    public void testSingleOperatorImmediatePill() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()

        def op = operator(inputs: [a, b, c], outputs: [d, e]) { x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        a << 10
        b << 20
        c << 30

        assert 60 == d.val
        assert 6000 == e.val
        a << PoisonPill.immediateInstance
        assert d.val == PoisonPill.immediateInstance
        assert e.val == PoisonPill.immediateInstance
        op.join()
    }

    public void testSplitter() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()

        def op = splitter(a, [d, e])

        a << 10

        assert 10 == d.val
        assert 10 == e.val
        a << PoisonPill.instance
        assert d.val == PoisonPill.instance
        assert e.val == PoisonPill.instance
        op.join()
    }

    public void testOperatorOnDFV() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable d = new DataflowVariable()

        def op = operator(inputs: [a], outputs: [d]) { x ->
            bindOutput x
        }

        a << PoisonPill.instance
        assert d.val == PoisonPill.instance
        op.join()
    }

    public void testOperatorNetwork() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()
        final DataflowQueue f = new DataflowQueue()
        final DataflowQueue out = new DataflowQueue()

        def op1 = operator(inputs: [a, b, c], outputs: [d, e]) { x, y, z -> }

        def op2 = operator(inputs: [d], outputs: [f, out]) {}

        def op3 = operator(inputs: [e, f], outputs: [b, out]) { x, y -> }

        a << PoisonPill.instance

        assert out.val == PoisonPill.instance
        assert out.val == PoisonPill.instance
        op1.join()
        op2.join()
        op3.join()
    }

    public void testSelectorNetworkWithFeedbackLoopAndNonImmediatePillNeverStops() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()
        final DataflowQueue f = new DataflowQueue()
        final DataflowQueue out = new DataflowQueue()

        def op1 = selector(inputs: [a, b, c], outputs: [d, e]) { value, index -> }

        def op2 = selector(inputs: [d], outputs: [f, out]) {}

        def op3 = prioritySelector(inputs: [e, f], outputs: [b]) { value, index -> }

        a << PoisonPill.instance
        sleep 500
        assert !out.bound

        b << PoisonPill.instance
        c << PoisonPill.instance

        assert out.val == PoisonPill.instance
        op1.join()
        op2.join()
        op3.join()
    }

    public void testSelectorNetworkImmediatePill() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()
        final DataflowQueue f = new DataflowQueue()
        final DataflowQueue out = new DataflowQueue()

        def op1 = selector(inputs: [a, b, c], outputs: [d, e]) { value, index -> }

        def op2 = selector(inputs: [d], outputs: [f, out]) {}

        def op3 = prioritySelector(inputs: [e, f], outputs: [b]) { value, index -> }

        a << PoisonPill.immediateInstance

        assert out.val == PoisonPill.immediateInstance
        op1.join()
        op2.join()
        op3.join()
    }
}