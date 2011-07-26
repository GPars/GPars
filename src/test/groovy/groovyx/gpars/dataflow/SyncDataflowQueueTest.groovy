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

import groovyx.gpars.actor.Actor
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import groovyx.gpars.group.NonDaemonPGroup

public abstract class SyncDataflowQueueTest extends GroovyTestCase {

    public void testWriterBlocking() {
        final SyncDataflowQueue queue = new SyncDataflowQueue()
        volatile boolean reached = false

        def t = Thread.start {
            queue << 10
            reached = true
            queue << 20
        }
        sleep 1000
        assert !reached
        assertEquals 10, queue.val
        assertEquals 20, queue.val
        t.join()
        assert queue.length() == 0
        assert reached
    }

    @SuppressWarnings("GroovyMethodWithMoreThanThreeNegations")
    public void testMultipleWriters() {
        final SyncDataflowQueue queue = new SyncDataflowQueue()
        volatile boolean reached1 = false
        volatile boolean reached2 = false

        def t1 = Thread.start {
            queue << 10
            queue << 20
            reached1 = true
        }
        def t2 = Thread.start {
            queue << 30
            queue << 40
            reached2 = true
        }
        sleep 1000
        assert !reached1
        assert !reached2
        assert queue.val in [10, 30]
        assert queue.val in [10, 30]

        assert !reached1
        assert !reached2
        assert queue.val in [20, 40]
        assert queue.val in [20, 40]

        [t1, t2]*.join()
        assert reached1
        assert reached2
    }

    public void testTimeoutGet() {
        final SyncDataflowQueue queue = new SyncDataflowQueue()
        assert queue.getVal(1, TimeUnit.SECONDS) == null
        Thread.start {queue << 10}
        assertEquals 10, queue.getVal(10, TimeUnit.SECONDS)
    }

    public void testAsyncRead() {
        final SyncDataflowQueue queue = new SyncDataflowQueue()

        def result1 = new DataflowVariable()
        def group = new NonDaemonPGroup(2)
        def actor = group.actor {
            react {
                result1 << it
            }
        }

        Thread.start {
            queue.getValAsync(actor)
        }

        def result2 = new DataflowVariable()
        Thread.start {
            queue.whenBound({result2 << it})
        }

        Thread.start {queue << 10}
        Thread.start {queue << 20}

        assert result1.val in [10, 20]
        assert result2.val in [10, 20]
        assert result1.val != result2.val
        group.shutdown()
    }

    public void testStreamPoll() {
        final SyncDataflowQueue stream = new SyncDataflowQueue()
        assert stream.poll() == null
        assert stream.poll() == null
        Thread.start {stream << 1}
        sleep 1000
        assert stream.poll()?.val == 1
        assert stream.poll()?.val == null
        Thread.start {stream << 2}
        sleep 1000
        assert stream.poll()?.val == 2
        assert stream.poll()?.val == null

        def group = new NonDaemonPGroup(2)
        final Actor thread = group.blockingActor {
            stream << 10
            final SyncDataflowVariable variable = new SyncDataflowVariable()
            stream << variable
            receive {
                variable << 20
            }
        }

        sleep 1000
        assertEquals 10, stream.poll()?.val
        assert stream.poll() == null
        thread << 'Proceed'
        assertEquals 20, stream.val
        assertEquals 0, stream.length()
        assert stream.poll() == null
        group.shutdown()
    }

    public void testNullValues() {
        final SyncDataflowQueue stream = new SyncDataflowQueue()
        def group = new NonDaemonPGroup(2)
        final Actor thread = group.blockingActor {
            stream << null
            final SyncDataflowVariable variable = new SyncDataflowVariable()
            stream << variable
            receive {
                variable << null
            }
        }

        assertNull stream.val
        thread << 'Proceed'
        assertNull stream.val
        assertEquals 0, stream.length()
        group.shutdown()
    }

    public void testIteration() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final SyncDataflowQueue stream = new SyncDataflowQueue()
        def group = new NonDaemonPGroup(2)
        final Actor thread = group.blockingActor {
            (0..10).each {num -> Thread.start {stream << num}}
            sleep 3000
            barrier.await()
            receive {
                stream << 11
            }
        }

        barrier.await()
        assertEquals 11, stream.length()
        stream.collect {it}.sort().eachWithIndex {element, index -> assert index == element }
        assertEquals 11, stream.length()

        thread << 'Proceed'
        (0..11).each {
            assert stream.val in (0..11)
        }
        group.shutdown()
    }

    public void testIterationWithNulls() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final SyncDataflowQueue stream = new SyncDataflowQueue()
        def group = new NonDaemonPGroup(2)
        group.blockingActor {
            (0..10).each {Thread.start {stream << null}}
            sleep 3000
            barrier.await()
        }

        barrier.await()
        assertEquals 11, stream.length()
        stream.each {assertNull it }
        assertEquals 11, stream.length()

        for (i in (0..10)) { assertNull stream.val }
        group.shutdown()
    }

    public void testToString() {
        final SyncDataflowQueue<Integer> stream = new SyncDataflowQueue<Integer>()
        assertEquals 'SyncDataflowQueue(queue=[])', stream.toString()
        Thread.start {stream << 10}
        sleep 1000
        assertEquals 'SyncDataflowQueue(queue=[SyncDataflowVariable(value=10)])', stream.toString()
        Thread.start {stream << 20}
        sleep 1000
        assertEquals 'SyncDataflowQueue(queue=[SyncDataflowVariable(value=10), SyncDataflowVariable(value=20)])', stream.toString()
        stream.val
        assertEquals 'SyncDataflowQueue(queue=[SyncDataflowVariable(value=20)])', stream.toString()
        stream.val
        assertEquals 'SyncDataflowQueue(queue=[])', stream.toString()

        final SyncDataflowVariable variable = new SyncDataflowVariable()
        Thread.start {stream << variable}
        sleep 1000
        assertEquals 'SyncDataflowQueue(queue=[SyncDataflowVariable(value=null)])', stream.toString()
        Thread.start {variable << '30'}
        Thread.sleep 3000  //let the value propagate asynchronously into the variable stored in the stream
        assertEquals 'SyncDataflowQueue(queue=[SyncDataflowVariable(value=30)])', stream.toString()
        assertEquals 'SyncDataflowQueue(queue=[SyncDataflowVariable(value=30)])', stream.toString()
        stream.val
        assertEquals 'SyncDataflowQueue(queue=[])', stream.toString()
        assertEquals 'SyncDataflowQueue(queue=[])', stream.toString()
    }

    public void testWhenBound() {
        final SyncDataflowQueue stream = new SyncDataflowQueue()
        final Dataflows df = new Dataflows()
        stream >> {df.x1 = it}
        stream >> {df.x2 = it}

        def group = new NonDaemonPGroup(2)
        def actor = group.actor {
            react {
                df.x3 = it
            }
        }
        stream.whenBound(actor)
        stream << 10
        stream << 20
        stream << 30
        assertEquals 10, df.x1
        assertEquals 20, df.x2
        assertEquals 30, df.x3
        group.shutdown()
    }

    public void testWheneverBound() {
        final SyncDataflowQueue stream = new SyncDataflowQueue()
        final SyncDataflowQueue dfs1 = new SyncDataflowQueue()
        final SyncDataflowQueue dfs2 = new SyncDataflowQueue()
        final SyncDataflowQueue dfs3 = new SyncDataflowQueue()
        stream.wheneverBound {dfs1 << it}
        stream.wheneverBound {dfs2 << it}

        def group = new NonDaemonPGroup(2)
        def actor = group.actor {
            react {
                dfs3 << it
                react {
                    dfs3 << it
                    react {
                        dfs3 << it
                        react {
                            dfs3 << it
                        }
                    }
                }
            }
        }
        stream.wheneverBound(actor)
        stream << 10
        stream << 20
        stream << 30
        def df = new SyncDataflowVariable()
        stream << df
        df << 40
        assert [10, 20, 30, 40] as Set == [dfs1.val, dfs1.val, dfs1.val, dfs1.val] as Set
        assert [10, 20, 30, 40] as Set == [dfs2.val, dfs2.val, dfs2.val, dfs2.val] as Set
        assert [10, 20, 30, 40] as Set == [dfs3.val, dfs3.val, dfs3.val, dfs3.val] as Set

        group.shutdown()
    }

    public void testAsyncValueRetrieval() {
        def result = new Dataflows()
        final SyncDataflowQueue stream = new SyncDataflowQueue()

        def group = new NonDaemonPGroup(2)
        group.actor {
            stream << 10
        }
        def handler = group.actor {
            react {result.value = it}
        }
        stream.getValAsync(handler)
        assert result.value == 10
        group.shutdown()
    }

    public void testGetValWithTimeout() {
        final SyncDataflowQueue stream = new SyncDataflowQueue()

        def group = new NonDaemonPGroup(2)
        group.actor {
            stream << 10
        }
        assert stream.getVal(10, TimeUnit.DAYS) == 10
        assert stream.getVal(3, TimeUnit.SECONDS) == null
        assert stream.getVal(3, TimeUnit.SECONDS) == null
        group.shutdown()
    }

    public void testMissedTimeout() {
        final SyncDataflowQueue stream = new SyncDataflowQueue()
        assertNull stream.getVal(10, TimeUnit.MILLISECONDS)
        Thread.start {stream << 10}
        assert 10 == stream.getVal(10, TimeUnit.SECONDS)
        Thread.start {stream << 20}
        assert 20 == stream.getVal(10, TimeUnit.SECONDS)
        Thread.start {stream << 30}
        assert 30 == stream.getVal(10, TimeUnit.SECONDS)
        assertNull stream.getVal(2, TimeUnit.SECONDS)
        Thread.start {stream << 40}
        assert 40 == stream.getVal(10, TimeUnit.SECONDS)
    }

    public void testMissedTimeoutWithNull() {
        final SyncDataflowQueue stream = new SyncDataflowQueue()
        assertNull stream.getVal(10, TimeUnit.MILLISECONDS)
        Thread.start {stream << null}
        assert null == stream.getVal(10, TimeUnit.MINUTES)
        Thread.start {stream << null}
        assert null == stream.getVal(10, TimeUnit.SECONDS)
        Thread.start {stream << 30}
        assert 30 == stream.getVal(10, TimeUnit.SECONDS)
        assertNull stream.getVal(2, TimeUnit.SECONDS)
        Thread.start {stream << null}
        assert null == stream.getVal(10, TimeUnit.SECONDS)
    }
}
