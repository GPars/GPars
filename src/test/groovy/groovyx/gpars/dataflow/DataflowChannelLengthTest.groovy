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

package groovyx.gpars.dataflow

import java.util.concurrent.CyclicBarrier

public class DataflowChannelLengthTest extends GroovyTestCase {

    public void testDFV() {
        final DataflowVariable variable = new DataflowVariable()
        assert variable.length() == 0
        variable << 10
        assert variable.length() == 1
    }

    public void testSyncDFV() {
        final DataflowVariable variable = new SyncDataflowVariable(1)
        assert variable.length() == 0
        Thread.start {
            variable << 10
        }
        sleep 3000
        assert variable.length() == 1
        variable.val
        assert variable.length() == 1
    }

    public void testDataflowQueue() {
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final DataflowQueue queue = new DataflowQueue()
        assert queue.length() == 0
        queue << 10
        assert queue.length() == 1
        queue << 20
        assert queue.length() == 2
        queue.val
        assert queue.length() == 1
        queue << 20
        assert queue.length() == 2
        queue.whenBound {barrier.await()}
        barrier.await()
        assert queue.length() == 1
        queue.val
        assert queue.length() == 0
    }

    public void testSyncDataflowQueue() {
        final DataflowQueue queue = new SyncDataflowQueue()
        assert queue.length() == 0
        Thread.start {
            queue << 10
        }
        sleep 3000
        assert queue.isBound()
        assert queue.length() == 1
        queue.val
        assert !queue.isBound()
        assert queue.length() == 0
    }

    public void testDataflowBroadcast() {
        final DataflowBroadcast channel = new DataflowBroadcast()
        final CyclicBarrier barrier = new CyclicBarrier(3)
        final DataflowReadChannel subscription1 = channel.createReadChannel()
        final DataflowReadChannel subscription2 = channel.createReadChannel()

        assert subscription1.length() == 0
        assert subscription2.length() == 0
        channel << 10
        assert subscription1.length() == 1
        assert subscription2.length() == 1
        channel << 20
        assert subscription1.length() == 2
        assert subscription2.length() == 2

        subscription1.val
        assert subscription1.length() == 1
        assert subscription2.length() == 2

        channel << 30
        assert subscription1.length() == 2
        assert subscription2.length() == 3

        subscription1.whenBound {barrier.await()}
        subscription2.whenBound {barrier.await()}
        barrier.await()
        assert subscription1.length() == 1
        assert subscription2.length() == 2

        subscription1.val
        subscription2.val
        assert subscription1.length() == 0
        assert subscription2.length() == 1

        subscription2.val
        assert subscription1.length() == 0
        assert subscription2.length() == 0
    }

    public void testDataflowBroadcastHeadAndAsyncHead() {
        final DataflowBroadcast channel = new DataflowBroadcast()
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final DataflowReadChannel subscription1 = channel.createReadChannel()
        final DataflowReadChannel subscription2 = channel.createReadChannel()
        channel << 1
        channel << 2
        channel << 3
        subscription1.whenBound {barrier.await()}
        assert 2 == subscription1.val
        assert 3 == subscription1.val
        barrier.await()
    }

    public void testSyncDataflowBroadcast() {
        final SyncDataflowBroadcast channel = new SyncDataflowBroadcast()
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final DataflowReadChannel subscription1 = channel.createReadChannel()
        final DataflowReadChannel subscription2 = channel.createReadChannel()

        assert subscription1.length() == 0
        assert subscription2.length() == 0
        Thread.start {
            channel << 10
        }
        sleep 3000
        assert subscription1.isBound()
        assert subscription1.length() == 1
        assert subscription2.isBound()
        assert subscription2.length() == 1
        Thread.start {
            channel << 20
        }
        sleep 3000
        assert subscription1.length() == 2
        assert subscription2.length() == 2

        def sub1IntermediateLength1 = new DataflowVariable()
        def sub1IntermediateLength2 = new DataflowVariable()
        Thread.start {
            subscription1.val
            barrier.await()
            sub1IntermediateLength1 << subscription1.length()
            barrier.await()
            subscription1.val
            sub1IntermediateLength2 << subscription1.length()
        }
        sleep 1000

        assert 2 == subscription2.length()
        subscription2.val

        barrier.await()
        assert sub1IntermediateLength1.val == 1
        assert subscription2.length() == 1
        barrier.await()
        subscription2.val
        assert sub1IntermediateLength2.val == 0
        assert subscription2.length() == 0
    }
}
