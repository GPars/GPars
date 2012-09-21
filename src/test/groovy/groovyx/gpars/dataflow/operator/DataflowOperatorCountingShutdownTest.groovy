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
import java.util.concurrent.TimeUnit
import static groovyx.gpars.dataflow.Dataflow.operator
import static groovyx.gpars.dataflow.Dataflow.prioritySelector
import static groovyx.gpars.dataflow.Dataflow.selector
import static groovyx.gpars.dataflow.Dataflow.splitter

/**
 * @author Vaclav Pech
 */

public class DataflowOperatorCountingShutdownTest extends GroovyTestCase {

    public void testSingleOperator() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()

        def op = operator(inputs: [a, b, c], outputs: [d, e]) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        a << 10
        b << 20
        c << 30

        assert 60 == d.val
        assert 6000 == e.val
        final pill = new CountingPoisonPill(1)
        a << pill
        pill.join()
        assert d.val == pill
        assert e.val == pill
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
        final pill = new CountingPoisonPill(1)
        a << pill
        pill.join()
        assert d.val == pill
        assert e.val == pill
        op.join()
    }

    public void testOperatorOnDFV() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable d = new DataflowVariable()

        def op = operator(inputs: [a], outputs: [d]) {x ->
            bindOutput x
        }

        final pill = new CountingPoisonPill(1)
        a << pill
        pill.join()
        assert d.val == pill
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

        def op1 = operator(inputs: [a, b, c], outputs: [d, e]) {x, y, z -> }

        def op2 = operator(inputs: [d], outputs: [f, out]) { }

        def op3 = operator(inputs: [e, f], outputs: [b, out]) {x, y -> }

        final pill = new CountingPoisonPill(3)
        a << pill
        pill.join()
        assert out.val == pill
        assert out.val == pill
        op1.join()
        op2.join()
        op3.join()
    }

    public void testSelectorNetwork() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()
        final DataflowQueue f = new DataflowQueue()
        final DataflowQueue out = new DataflowQueue()

        def op1 = selector(inputs: [a, b, c], outputs: [d, e]) {value, index -> }

        def op2 = selector(inputs: [d], outputs: [f, out]) { }

        def op3 = prioritySelector(inputs: [e, f], outputs: [b]) {value, index -> }

        final pill = new ImmediateCountingPoisonPill(3)
        a << pill
        pill.join()
        assert out.val == pill
        op1.join()
        op2.join()
        op3.join()
    }

    public void testTerminationProperty() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()
        final DataflowQueue e = new DataflowQueue()

        def op = operator(inputs: [a, b, c], outputs: [d, e]) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        a << 10
        b << 20
        c << 30

        assert 60 == d.val
        assert 6000 == e.val
        final pill = new CountingPoisonPill(1)
        assert !pill.termination.bound
        a << pill
        pill.join()
        assert pill.termination.bound
        op.join()
    }

    public void testTerminationWithRepetition() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = operator(inputs: [a, b, c], outputs: [d]) {x, y, z ->
            bindOutput 0, x + y + z
        }

        a << 10
        b << 20
        c << 30

        assert 60 == d.val
        final pill = new CountingPoisonPill(2)
        a << pill
        c << pill
        pill.join(1, TimeUnit.SECONDS)
        assert !pill.termination.bound
        op.join()
    }

    public void testTerminationWithRepetitionOnSelector() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = selector(inputs: [a, b, c], outputs: [d]) {x ->
            bindOutput 0, x
        }

        a << 10
        b << 20
        c << 30

        assert (1..3).collect {d.val} as Set == [10, 20, 30] as Set
        final pill = new ImmediateCountingPoisonPill(2)
        a << pill
        c << pill
        pill.join(1, TimeUnit.SECONDS)
        assert !pill.termination.bound
        op.join()
    }

    public void testNonImmediateTerminationWithRepetitionOnSelector() {
        final DataflowQueue a = new DataflowQueue()
        final DataflowQueue b = new DataflowQueue()
        final DataflowQueue c = new DataflowQueue()
        final DataflowQueue d = new DataflowQueue()

        def op = selector(inputs: [a, b, c], outputs: [d]) {x ->
            bindOutput 0, x
        }

        a << 10
        b << 20
        c << 30

        assert (1..3).collect {d.val} as Set == [10, 20, 30] as Set
        final pill = new CountingPoisonPill(1)
        a << pill
        c << pill
        pill.join(1, TimeUnit.SECONDS)
        assert !pill.termination.bound

        b << pill
        pill.join()
        op.join()
    }
}