// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.util

import java.util.concurrent.CyclicBarrier

abstract class AbstractMessageQueueTest extends GroovyTestCase {

    protected abstract MessageQueue createMessageQueue()

    ;

    public void testEmptyQueue() {
        final def queue = createMessageQueue()
        assert queue.isEmpty()
        assertNull queue.poll()
        assertNull queue.poll()

        queue.add 1
        assert !queue.isEmpty()
        assert 1 == queue.poll()
        assert queue.isEmpty()
        assertNull queue.poll()
    }

    public void testOrder() {
        final def queue = createMessageQueue()
        queue.add 1
        queue.add 2
        queue.add 3
        queue.add 1
        queue.add 4

        assert !queue.isEmpty()
        assert 1 == queue.poll()
        assert !queue.isEmpty()
        assert 2 == queue.poll()
        assert !queue.isEmpty()
        assert 3 == queue.poll()
        assert 1 == queue.poll()
        assert 4 == queue.poll()
        assert queue.isEmpty()
        assertNull queue.poll()
    }

    public void testThreading() {
        final def queue = createMessageQueue()
        long sum = 0L
        long targetSum = (1..400).sum()

        final def barrier = new CyclicBarrier(6)

        def writer1 = Thread.start {
            barrier.await()
            (1..100).each {queue.add it}
        }
        def writer2 = Thread.start {
            barrier.await()
            (101..200).each {queue.add it}
        }
        def writer3 = Thread.start {
            barrier.await()
            (201..300).each {queue.add it}
        }
        def writer4 = Thread.start {
            barrier.await()
            (301..400).each {queue.add it}
        }
        def reader = Thread.start {
            barrier.await()
            while (sum < targetSum) {
                final def value = queue.poll()
                if (value != null) sum += value
                else Thread.yield()
            }
        }

        barrier.await()
        [writer1, writer2, writer3, writer4, reader]*.join()
        assert sum == targetSum
    }
}
