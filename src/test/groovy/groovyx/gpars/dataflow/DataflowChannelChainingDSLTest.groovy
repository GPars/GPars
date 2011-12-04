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
import groovyx.gpars.group.PGroup

/**
 * @author Vaclav Pech
 */
class DataflowChannelChainingDSLTest extends GroovyTestCase {
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

    public void testIntoQueue() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()
        queue1.chainWith(group) {it * 2}.into(group, queue2)


        queue1 << 1
        queue1 << 2
        queue1 << 3
        queue1 << 4
        queue1 << 5

        assert 2 == queue2.val
        assert 4 == queue2.val
        assert 6 == queue2.val
        assert 8 == queue2.val
        assert 10 == queue2.val
    }

    public void testSplitQueue() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()
        final DataflowQueue queue3 = new DataflowQueue()
        queue1.chainWith(group) {it * 2}.split(group, queue2, queue3)


        queue1 << 1
        queue1 << 2
        queue1 << 3
        queue1 << 4
        queue1 << 5

        [queue2, queue3].each {
            assert 2 == it.val
            assert 4 == it.val
            assert 6 == it.val
            assert 8 == it.val
            assert 10 == it.val
        }
    }

    public void testIntoBroadcast() {
        final DataflowBroadcast broadcast1 = new DataflowBroadcast()
        final DataflowBroadcast broadcast2 = new DataflowBroadcast()
        broadcast1.createReadChannel().chainWith(group) {it * 2}.into(group, broadcast2)


        final subscription = broadcast2.createReadChannel()

        broadcast1 << 1
        broadcast1 << 2
        broadcast1 << 3
        broadcast1 << 4
        broadcast1 << 5

        assert 2 == subscription.val
        assert 4 == subscription.val
        assert 6 == subscription.val
        assert 8 == subscription.val
        assert 10 == subscription.val
    }

    public void testSplitBroadcast() {
        final DataflowBroadcast broadcast1 = new DataflowBroadcast()
        final DataflowBroadcast broadcast2 = new DataflowBroadcast()
        final DataflowBroadcast broadcast3 = new DataflowBroadcast()
        broadcast1.createReadChannel().chainWith(group) {it * 2}.split(group, broadcast2, broadcast3)

        final DataflowReadChannel subscription2 = broadcast2.createReadChannel()
        final DataflowReadChannel subscription3 = broadcast3.createReadChannel()

        broadcast1 << 1
        broadcast1 << 2
        broadcast1 << 3
        broadcast1 << 4
        broadcast1 << 5

        [subscription2, subscription3].each {
            assert 2 == it.val
            assert 4 == it.val
            assert 6 == it.val
            assert 8 == it.val
            assert 10 == it.val
        }
    }

    public void testIntoDFV() {
        final DataflowVariable variable1 = new DataflowVariable()
        final DataflowVariable variable2 = new DataflowVariable()
        variable1.chainWith(group) {it * 2}.into(group, variable2)


        variable1 << 1

        assert 2 == variable2.val
    }

    public void testSplitDFV() {
        final DataflowVariable variable1 = new DataflowVariable()
        final DataflowVariable variable2 = new DataflowVariable()
        final DataflowVariable variable3 = new DataflowVariable()
        variable1.chainWith(group) {it * 2}.split(group, variable2, variable3)


        variable1 << 1

        [variable2, variable3].each {
            assert 2 == it.val
        }
    }

    public void testDelegatePropagation() {
        final DataflowQueue queue = new DataflowQueue()
        def result = queue.chainWith {
            bindOutput 10
            return 2
        }
        queue << 1
        assert 10 == result.val
        assert 2 == result.val
    }
}