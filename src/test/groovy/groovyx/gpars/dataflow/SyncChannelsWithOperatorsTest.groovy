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

import groovyx.gpars.group.NonDaemonPGroup

class SyncChannelsWithOperatorsTest extends GroovyTestCase {
    public void testOperators() {
        final DataflowReadChannel queue = new SyncDataflowQueue()
        final DataflowReadChannel result = new SyncDataflowQueue()
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final DataflowReadChannel subscription = broadcast.createReadChannel()

        Thread.start {
            queue << 1
            queue << 2
            queue << 3
            queue << 4
            queue << 5
        }

        final NonDaemonPGroup group = new NonDaemonPGroup()
        group.operator(queue, broadcast) {value ->
            bindOutput(2 * value)
        }
        group.operator(subscription, result) {value ->
            bindOutput(value + 1)
        }

        def expectedValues = [3, 5, 7, 9, 11] as Set
        5.times {
            final Object value = result.val
            assert value in expectedValues
            expectedValues.remove(value)
        }
    }

    public void testSelectors() {
        final DataflowReadChannel queue = new SyncDataflowQueue()
        final DataflowReadChannel result = new SyncDataflowQueue()
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final DataflowReadChannel subscription = broadcast.createReadChannel()

        Thread.start {
            queue << 1
            queue << 2
            queue << 3
            queue << 4
            queue << 5
        }

        final NonDaemonPGroup group = new NonDaemonPGroup(2)
        group.selector([queue], [broadcast]) {value ->
            bindOutput(2 * value)
        }
        group.selector([subscription], [result]) {value ->
            bindOutput(value + 1)
        }

        def expectedValues = [3, 5, 7, 9, 11] as Set
        5.times {
            final Object value = result.val
            assert value in expectedValues
            expectedValues.remove(value)
        }
    }
}
