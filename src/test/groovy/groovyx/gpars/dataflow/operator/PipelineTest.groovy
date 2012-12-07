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

import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.group.PGroup

/**
 * @author Vaclav Pech
 */
public class PipelineTest extends GroovyTestCase {

    private PGroup group

    public void setUp() throws Exception {
        group = new NonDaemonPGroup()
    }

    public void tearDown() throws Exception {
        group.shutdown()
    }

    public void testPipeline() {
        final DataflowQueue queue = new DataflowQueue()
        final DataflowQueue result = new DataflowQueue()
        final Pipeline pipeline = new Pipeline(group, queue)

        assert !pipeline.complete
        pipeline | { it * 2 } | { it + 1 } | result
        assert pipeline.complete
        shouldFail(IllegalStateException) {
            pipeline | result
        }

        queue << 1
        queue << 2
        queue << 3

        assert 3 == result.val
        assert 5 == result.val
        assert 7 == result.val
    }

    public void testPipelineWithCustomizedGroup() {
        final DataflowQueue queue = new DataflowQueue()
        final DataflowQueue groups = new DataflowQueue()
        final DataflowQueue result = new DataflowQueue()
        Dataflow.usingGroup(group) {
            final Pipeline pipeline = new Pipeline(queue)

            assert !pipeline.complete
            pipeline | { groups << delegate.actor.parallelGroup; it * 2 } | { groups << delegate.actor.parallelGroup; it + 1 } | result
            assert pipeline.complete
            shouldFail(IllegalStateException) {
                pipeline | result
            }

            queue << 1
            queue << 2
            queue << 3
        }

        assert 3 == result.val
        assert 5 == result.val
        assert 7 == result.val
        2.times {
            assert group == groups.val
        }
    }

    public void testPipelineOutput() {
        final DataflowQueue queue = new DataflowQueue()
        final Pipeline pipeline = new Pipeline(group, queue)

        assert !pipeline.complete
        pipeline | { it * 2 } | { it + 1 }
        assert !pipeline.complete

        queue << 1
        queue << 2
        queue << 3

        assert 3 == pipeline.output.val
        assert 5 == pipeline.output.val
        assert 7 == pipeline.output.val
    }

    public void testSplit() {
        final DataflowQueue queue = new DataflowQueue()
        final DataflowQueue result1 = new DataflowQueue()
        final DataflowQueue result2 = new DataflowQueue()
        final Pipeline pipeline = new Pipeline(group, queue)

        assert !pipeline.complete
        pipeline | { it * 2 } | { it + 1 }
        assert !pipeline.complete
        pipeline.split(result1, result2)
        assert pipeline.complete

        shouldFail(IllegalStateException) {
            pipeline | new DataflowQueue()
        }

        queue << 1
        queue << 2
        queue << 3

        assert 3 == result1.val
        assert 5 == result1.val
        assert 7 == result1.val
        assert 3 == result2.val
        assert 5 == result2.val
        assert 7 == result2.val
    }

    public void testTap() {
        final DataflowQueue queue = new DataflowQueue()
        final DataflowQueue result1 = new DataflowQueue()
        final DataflowQueue result2 = new DataflowQueue()
        final Pipeline pipeline = new Pipeline(group, queue)

        assert !pipeline.complete
        pipeline | { it * 2 } | { it + 1 }
        assert !pipeline.complete
        pipeline.tap(result1)
        assert !pipeline.complete
        pipeline.into result2
        assert pipeline.complete

        shouldFail(IllegalStateException) {
            pipeline | new DataflowQueue()
        }

        queue << 1
        queue << 2
        queue << 3

        assert 3 == result1.val
        assert 5 == result1.val
        assert 7 == result1.val
        assert 3 == result2.val
        assert 5 == result2.val
        assert 7 == result2.val
    }

    public void testMerge() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()
        final DataflowQueue queue3 = new DataflowQueue()
        new Pipeline(group, queue1).merge(queue2) { a, b -> a + b }.into queue3

        queue1 << 1
        queue1 << 2
        queue2 << 3
        queue2 << 4

        assert 4 == queue3.val
        assert 6 == queue3.val
    }

    public void testMergeWithAdditionalParameters() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()
        final DataflowQueue queue3 = new DataflowQueue()

        final DataflowQueue fromListener = new DataflowQueue()
        def listener = new DataflowEventAdapter() {
            @Override
            Object messageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
                fromListener << 'Message arrived'
                return super.messageArrived(processor, channel, index, message)
            }
        }

        new Pipeline(group, queue1).merge([maxForks: 1, listeners: [listener]], queue2) { a, b -> a + b }.into queue3

        queue1 << 1
        queue1 << 2
        queue2 << 3
        queue2 << 4

        assert 4 == queue3.val
        assert 6 == queue3.val

        2.times {
            assert 'Message arrived' == fromListener.val
        }
    }

    public void testMergeMultiple() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()
        final DataflowQueue queue3 = new DataflowQueue()
        final DataflowQueue queue4 = new DataflowQueue()
        new Pipeline(group, queue1).merge([queue2, queue3]) { a, b, c -> a + b + c }.into queue4

        queue1 << 1
        queue1 << 2
        queue2 << 3
        queue2 << 4
        queue3 << 10
        queue3 << 10

        assert 14 == queue4.val
        assert 16 == queue4.val
    }

    public void testBinaryChoice() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()
        final DataflowQueue queue3 = new DataflowQueue()
        new Pipeline(group, queue1).binaryChoice(queue2, queue3) { a -> a >= 0 }

        queue1 << 1
        queue1 << 2
        queue1 << 3
        queue1 << 4
        queue1 << -10
        queue1 << -20

        assert 1 == queue2.val
        assert 2 == queue2.val
        assert 3 == queue2.val
        assert 4 == queue2.val
        assert -10 == queue3.val
        assert -20 == queue3.val
    }

    public void testChoice() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()
        final DataflowQueue queue3 = new DataflowQueue()
        final DataflowQueue queue4 = new DataflowQueue()
        new Pipeline(group, queue1).choice([queue2, queue3, queue4]) { a -> a % 3 }

        queue1 << 0
        queue1 << 1
        queue1 << 2
        queue1 << 3
        queue1 << 4
        queue1 << 5

        assert 0 == queue2.val
        assert 3 == queue2.val
        assert 1 == queue3.val
        assert 4 == queue3.val
        assert 2 == queue4.val
        assert 5 == queue4.val
        assert !queue2.isBound()
        assert !queue3.isBound()
        assert !queue4.isBound()
    }

    public void testSeparation() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()
        final DataflowQueue queue3 = new DataflowQueue()
        final DataflowQueue queue4 = new DataflowQueue()
        new Pipeline(group, queue1).separate([queue2, queue3, queue4]) { a -> [a - 1, a, a + 1] }

        queue1 << 1
        queue1 << 2
        queue1 << 3

        assert 0 == queue2.val
        assert 1 == queue2.val
        assert 2 == queue2.val
        assert 1 == queue3.val
        assert 2 == queue3.val
        assert 3 == queue3.val
        assert 2 == queue4.val
        assert 3 == queue4.val
        assert 4 == queue4.val
        assert !queue2.isBound()
        assert !queue3.isBound()
        assert !queue4.isBound()
    }
}
