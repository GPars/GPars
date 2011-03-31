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

package groovyx.gpars.dataflow

import groovyx.gpars.dataflow.operator.DataFlowPoisson
import static groovyx.gpars.dataflow.DataFlow.operator

/**
 * Created by IntelliJ IDEA.
 * User: Vaclav
 * Date: 30.3.11
 * Time: 14:28
 * To change this template use File | Settings | File Templates.
 */
class DataFlowChannelMapperTest extends GroovyTestCase {

    public void testCreationFromQueue() {
        final DataFlowQueue queue = new DataFlowQueue()
        final DataFlowReadChannel filteredQueue = new DataFlowQueue()
        operator(queue, filteredQueue) {
            bindOutput it * 2
        }
        queue.bind 1
        queue.bind 2
        queue.bind 3
        assert 2 == filteredQueue.val
        assert 4 == filteredQueue.val
        assert 6 == filteredQueue.val
        assert !filteredQueue.isBound()
        queue.bind(DataFlowPoisson.instance)
    }

    public void testCreationFromBroadCast() {
        final DataFlowBroadcast queue = new DataFlowBroadcast()
        final DataFlowReadChannel filteredQueue = new DataFlowQueue()
        operator(queue.createReadChannel(), filteredQueue) {
            bindOutput it * 2
        }
        queue.bind 1
        queue.bind 2
        queue.bind 3
        assert 2 == filteredQueue.val
        assert 4 == filteredQueue.val
        assert 6 == filteredQueue.val
        assert !filteredQueue.isBound()
        queue.bind(DataFlowPoisson.instance)
    }
}
