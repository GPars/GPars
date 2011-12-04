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
        final Pipeline pipeline = new Pipeline(queue)

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

    public void testSplit() {
        final DataflowQueue queue = new DataflowQueue()
        final DataflowQueue result1 = new DataflowQueue()
        final DataflowQueue result2 = new DataflowQueue()
        final Pipeline pipeline = new Pipeline(queue)

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
}
