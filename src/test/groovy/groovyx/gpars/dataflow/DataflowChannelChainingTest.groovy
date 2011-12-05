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

import groovyx.gpars.dataflow.operator.PoisonPill
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.group.PGroup

/**
 * @author Vaclav Pech
 */
class DataflowChannelChainingTest extends GroovyTestCase {
    private PGroup group

    @Override
    protected void setUp() {
        super.setUp()
        group = new NonDaemonPGroup();
    }

    @Override
    protected void tearDown() {
        super.tearDown()
        group.shutdown()
        group = null
    }

    public void testQueue() {
        final DataflowQueue queue = new DataflowQueue()
        final result = queue.chainWith(group) {it * 2}.chainWith(group) {it + 1}.chainWith(group) {it}

        queue << 1
        queue << 2
        queue << 3
        queue << 4
        queue << 5

        assert 3 == result.val
        assert 5 == result.val
        assert 7 == result.val
        assert 9 == result.val
        assert 11 == result.val
    }

    public void testQueueWithOrOperator() {
        final DataflowQueue queue = new DataflowQueue()
        final result = queue | {it * 2} | {it + 1} | {it}

        queue << 1
        queue << 2
        queue << 3
        queue << 4
        queue << 5

        assert 3 == result.val
        assert 5 == result.val
        assert 7 == result.val
        assert 9 == result.val
        assert 11 == result.val
        queue << PoisonPill.instance
    }

    public void testClosureArguments() {
        final DataflowQueue queue = new DataflowQueue()
        final DataflowVariable result = new DataflowVariable()

        shouldFail(IllegalArgumentException) {
            queue.chainWith {a, b ->}
        }
        shouldFail(IllegalArgumentException) {
            queue.chainWith {a, b, c ->}
        }
        shouldFail(IllegalArgumentException) {
            queue.chainWith {-> result << 1}
        }
    }

    public void testSyncQueue() {
        final SyncDataflowQueue queue = new SyncDataflowQueue()
        final result = queue.chainWith(group) {it * 2}.chainWith(group) {it + 1}.chainWith(group) {it}

        final resultQueue = new DataflowQueue()
        Thread.start {
            5.times {
                resultQueue << result.val
            }
        }

        queue << 1
        queue << 2
        queue << 3
        queue << 4
        queue << 5

        assert 3 == resultQueue.val
        assert 5 == resultQueue.val
        assert 7 == resultQueue.val
        assert 9 == resultQueue.val
        assert 11 == resultQueue.val
    }

    public void testDFV() {
        final DataflowVariable variable = new DataflowVariable()
        final result = variable.chainWith(group) {it * 2}.chainWith(group) {it + 1}.chainWith(group) {it}

        variable << 1

        assert 3 == result.val
    }

    public void testSyncDFV() {
        final SyncDataflowVariable variable = new SyncDataflowVariable()
        final result = variable.chainWith(group) {it * 2}.chainWith(group) {it + 1}.chainWith(group) {it}

        final resultQueue = new DataflowQueue()
        Thread.start {
            resultQueue << result.val
        }

        variable << 1

        assert 3 == resultQueue.val
    }

    public void testBroadcastSubscription() {
        final DataflowBroadcast broadcast = new DataflowBroadcast()
        final result = broadcast.createReadChannel().chainWith(group) {it * 2}.chainWith(group) {it + 1}.chainWith(group) {it}

        broadcast << 1
        broadcast << 2
        broadcast << 3
        broadcast << 4
        broadcast << 5

        assert 3 == result.val
        assert 5 == result.val
        assert 7 == result.val
        assert 9 == result.val
        assert 11 == result.val
    }

    public void testSyncBroadcastSubscription() {
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final result = broadcast.createReadChannel().chainWith(group) {it * 2}.chainWith(group) {it + 1}.chainWith(group) {it}

        final resultQueue = new DataflowQueue()
        Thread.start {
            5.times {
                resultQueue << result.val
            }
        }

        broadcast << 1
        broadcast << 2
        broadcast << 3
        broadcast << 4
        broadcast << 5

        assert 3 == resultQueue.val
        assert 5 == resultQueue.val
        assert 7 == resultQueue.val
        assert 9 == resultQueue.val
        assert 11 == resultQueue.val
    }
}
