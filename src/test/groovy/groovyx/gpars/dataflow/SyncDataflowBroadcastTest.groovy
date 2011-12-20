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

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class SyncDataflowBroadcastTest extends GroovyTestCase {
    public void testBlocking() {
        final SyncDataflowBroadcast<Integer> broadcast = new SyncDataflowBroadcast<Integer>()
        final AtomicBoolean writerReached = new AtomicBoolean(false)
        final AtomicInteger readerReached = new AtomicInteger(0)
        def subscription1 = broadcast.createReadChannel()
        def subscription2 = broadcast.createReadChannel()

        def t1 = Thread.start {
            broadcast << 10
            writerReached.set(true)
        }

        def t2 = Thread.start {
            readerReached.set(subscription1.val)
        }

        sleep 1000
        assert !writerReached.get()
        assert readerReached.get() == 0

        assert subscription2.val == 10
        [t1, t2]*.join()
        assert writerReached.get()
        assert readerReached.get() == 10
    }

    public void testBlockingWithDelayedSubscription() {
        final SyncDataflowBroadcast<Integer> broadcast = new SyncDataflowBroadcast<Integer>()
        final AtomicBoolean writerReached = new AtomicBoolean(false)
        final AtomicInteger readerReached = new AtomicInteger(0)
        final DataflowReadChannel<Integer> subscription2 = broadcast.createReadChannel()

        final CyclicBarrier barrier = new CyclicBarrier(2)
        def t1 = Thread.start {
            barrier.await()
            broadcast << 10
            writerReached.set(true)
        }

        def t2 = Thread.start {
            def subscription1 = broadcast.createReadChannel()
            barrier.await()
            readerReached.set(subscription2.val)
        }

        sleep 1000
        assert !writerReached.get()
        assert readerReached.get() == 0

        assert subscription2.val == 10

        [t1, t2]*.join()
        assert writerReached.get()
        assert readerReached.get() == 10
    }

    public void testGraduallyIncomingSubscription() {
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final AtomicBoolean writerReached1 = new AtomicBoolean(false)
        final AtomicBoolean writerReached2 = new AtomicBoolean(false)
        broadcast << 1
        final DataflowReadChannel subscription1 = broadcast.createReadChannel()

        def t1 = Thread.start {
            broadcast << 2
            writerReached1.set(true)
        }

        sleep 1000
        assert !writerReached1.get()

        assert subscription1.val == 2

        final DataflowReadChannel subscription2 = broadcast.createReadChannel()

        def t2 = Thread.start {
            broadcast << 3
            writerReached2.set(true)
        }

        sleep 1000
        assert !writerReached2.get()

        def readerResult = new DataflowVariable()
        Thread.start {readerResult << subscription1.val}
        assert subscription2.val == 3
        assert readerResult.val == 3

        [t1, t2]*.join()
        assert writerReached1.get()
        assert writerReached2.get()
    }

    public void testMultipleWriters() {
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final DataflowReadChannel subscription = broadcast.createReadChannel()
        (1..20).each {num -> Thread.start {broadcast << num}}
        sleep 1000

        def results = [] as Set
        20.times {
            final Object value = subscription.val
            assert value in (1..20)
            assert !results.contains(value)
            results << value
        }
        assert results.size() == 20

    }

    public void testEarlyUnSubscribing() {
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final AtomicBoolean writerReached = new AtomicBoolean(false)
        final DataflowReadChannel subscription1 = broadcast.createReadChannel()
        final DataflowReadChannel subscription2 = broadcast.createReadChannel()
        final DataflowReadChannel subscription3 = broadcast.createReadChannel()

        broadcast.unsubscribeReadChannel(subscription1)

        def t1 = Thread.start {
            broadcast << 1
            writerReached.set(true)
        }

        sleep 1000
        assert !writerReached.get()

        Thread.start {subscription2.val}
        assert subscription3.val == 1
        shouldFail(IllegalStateException) {
            subscription1.val
        }
        shouldFail(IllegalStateException) {
            subscription1.bound
        }
        shouldFail(IllegalStateException) {
            subscription1.whenBound {}
        }
        shouldFail(IllegalStateException) {
            subscription1.wheneverBound {}
        }

        t1.join()
        assert writerReached.get()
    }

    public void testUnSubscribing() {
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final AtomicBoolean writerReached = new AtomicBoolean(false)
        def barrier = new CyclicBarrier(3)

        final DataflowReadChannel subscription1 = broadcast.createReadChannel()
        final DataflowReadChannel subscription2 = broadcast.createReadChannel()
        final DataflowReadChannel subscription3 = broadcast.createReadChannel()

        Thread.start {
            broadcast << 1
        }
        Thread.start {
            subscription2.val
            barrier.await()
        }
        Thread.start {
            subscription1.val
            barrier.await()
        }

        assert subscription3.val == 1
        barrier.await()

        broadcast.unsubscribeReadChannel(subscription2)

        def t1 = Thread.start {
            broadcast << 2
            writerReached.set(true)
        }

        sleep 1000
        assert !writerReached.get()

        broadcast.unsubscribeReadChannel(subscription3)

        assert subscription1.val == 2

        shouldFail(IllegalStateException) {
            subscription2.val
        }
        shouldFail(IllegalStateException) {
            subscription3.val
        }

        t1.join()
        assert writerReached.get()
    }

    public void testDoubleUnSubscribingNotAllowed() {
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final DataflowReadChannel subscription1 = broadcast.createReadChannel()

        broadcast.unsubscribeReadChannel(subscription1)
        shouldFail(IllegalStateException) {
            broadcast.unsubscribeReadChannel(subscription1)
        }
    }

    public void testSubscribingWithAsyncOperations() {
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final AtomicBoolean writerReached1 = new AtomicBoolean(false)
        final AtomicBoolean writerReached2 = new AtomicBoolean(false)
        final DataflowReadChannel subscription1 = broadcast.createReadChannel()
        final DataflowReadChannel subscription2 = broadcast.createReadChannel()
        final DataflowReadChannel subscription3 = broadcast.createReadChannel()

        def t1 = Thread.start {
            broadcast << 1
            writerReached1.set(true)
        }

        def result1 = new DataflowVariable()
        def result2 = new DataflowVariable()
        subscription1.whenBound {
            result1 << it
        }
        subscription2.whenBound {}
        assert subscription3.val == 1
        assert result1.val == 1
        t1.join()
        assert writerReached1.get()



        subscription2.whenBound {
            result2 << it
        }
        broadcast.unsubscribeReadChannel(subscription2)

        def t2 = Thread.start {
            broadcast << 2
            writerReached2.set(true)
        }

        sleep 1000
        assert !writerReached2.get()

        broadcast.unsubscribeReadChannel(subscription1)

        assert subscription3.val == 2
        assert result2.val == 2

        shouldFail(IllegalStateException) {
            subscription2.val
        }
        shouldFail(IllegalStateException) {
            subscription1.val
        }

        t2.join()
        assert writerReached2.get()
    }

    public void testSubscribingWithMultipleAsyncOperations() {
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final AtomicBoolean writerReached1 = new AtomicBoolean(false)
        final AtomicBoolean writerReached2 = new AtomicBoolean(false)
        final DataflowReadChannel subscription1 = broadcast.createReadChannel()
        final DataflowReadChannel subscription2 = broadcast.createReadChannel()

        def t1 = Thread.start {
            broadcast << 1
            writerReached1.set(true)
            broadcast << 2
            writerReached2.set(true)
        }

        def result1 = new DataflowVariable()
        def result2 = new DataflowVariable()
        subscription1.whenBound {
            result1 << it
        }
        subscription1.whenBound {
            result2 << it
        }
        broadcast.unsubscribeReadChannel(subscription1)

        assert subscription2.val == 1
        assert result1.val == 1
        sleep 1000
        assert writerReached1.get()
        assert !writerReached2.get()
        assert subscription2.val == 2
        assert result2.val == 2

        t1.join()
        assert writerReached2.get()

        Thread.start {
            broadcast << 3
        }
        assert subscription2.val == 3
    }

    public void testWheneverBound() {
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        final AtomicBoolean writerReached1 = new AtomicBoolean(false)
        final AtomicBoolean writerReached2 = new AtomicBoolean(false)
        final DataflowReadChannel subscription1 = broadcast.createReadChannel()
        final DataflowReadChannel subscription2 = broadcast.createReadChannel()
        final DataflowReadChannel subscription3 = broadcast.createReadChannel()

        def result1 = new DataflowQueue()
        subscription1.wheneverBound {
            result1 << it
        }

        def t1 = Thread.start {
            broadcast << 1
            writerReached1.set(true)
        }

        subscription2.whenBound {}
        assert subscription3.val == 1
        assert result1.val == 1
        t1.join()
        assert writerReached1.get()


        broadcast.unsubscribeReadChannel(subscription2)
        shouldFail(IllegalStateException) {
            broadcast.unsubscribeReadChannel(subscription1)
        }
        shouldFail(IllegalStateException) {
            subscription2.wheneverBound { }
        }


        def t2 = Thread.start {
            broadcast << 2
            writerReached2.set(true)
        }

        sleep 1000
        assert !writerReached2.get()

        shouldFail(IllegalStateException) {
            broadcast.unsubscribeReadChannel(subscription2)
        }

        assert subscription3.val == 2
        assert result1.val == 2

        shouldFail(IllegalStateException) {
            subscription2.val
        }

        t2.join()
        assert writerReached2.get()
    }

    public void testTimeoutGetWithMultipleParties() {
        final SyncDataflowBroadcast broadcast = new SyncDataflowBroadcast()
        DataflowReadChannel subscription1 = broadcast.createReadChannel()
        DataflowReadChannel subscription2 = broadcast.createReadChannel()
        Thread.start {broadcast << 10}

        assert null == subscription1.getVal(1, TimeUnit.MILLISECONDS)
        assert null == subscription1.getVal(1, TimeUnit.MILLISECONDS)
        assert null == subscription2.getVal(1, TimeUnit.MILLISECONDS)
        assert null == subscription1.getVal(1, TimeUnit.MILLISECONDS)

        Thread.start {
            subscription1.getVal(10, TimeUnit.SECONDS)
        }
        assert 10 == subscription2.getVal(10, java.util.concurrent.TimeUnit.SECONDS)
    }

}
