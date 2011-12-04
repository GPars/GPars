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
        pipeline | {it * 2} | {it + 1} | result
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

    public void testPipelineOutput() {
        final DataflowQueue queue = new DataflowQueue()
        final Pipeline pipeline = new Pipeline(group, queue)

        assert !pipeline.complete
        pipeline | {it * 2} | {it + 1}
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
        pipeline | {it * 2} | {it + 1}
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
        pipeline | {it * 2} | {it + 1}
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
        new Pipeline(group, queue1).merge(queue2) {a, b -> a + b}.into queue3

        queue1 << 1
        queue1 << 2
        queue2 << 3
        queue2 << 4

        assert 4 == queue3.val
        assert 6 == queue3.val
    }

    public void testMergeMultiple() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()
        final DataflowQueue queue3 = new DataflowQueue()
        final DataflowQueue queue4 = new DataflowQueue()
        new Pipeline(group, queue1).merge([queue2, queue3]) {a, b, c -> a + b + c}.into queue4

        queue1 << 1
        queue1 << 2
        queue2 << 3
        queue2 << 4
        queue3 << 10
        queue3 << 10

        assert 14 == queue4.val
        assert 16 == queue4.val
    }

}
