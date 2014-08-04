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
import groovyx.gpars.actor.Actors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

public class DataflowQueueTest extends GroovyTestCase {

    public void testStream() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataflowQueue stream = new DataflowQueue()
        final Actor thread = Actors.blockingActor {
            stream << 10
            final DataflowVariable variable = new DataflowVariable()
            stream << variable
            latch.countDown()
            receive {
                variable << 20
            }
        }

        latch.await()
        assert 2 == stream.length()
        assert 10 == stream.val
        assert 1 == stream.length()
        thread << 'Proceed'
        assert 20 == stream.val
        assert 0 == stream.length()
    }

    public void testStreamPoll() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataflowQueue stream = new DataflowQueue()
        assert stream.poll() == null
        assert stream.poll() == null
        stream << 1
        assert stream.poll()?.val == 1
        assert stream.poll()?.val == null
        stream << 2
        assert stream.poll()?.val == 2
        assert stream.poll()?.val == null
        final Actor thread = Actors.blockingActor {
            stream << 10
            final DataflowVariable variable = new DataflowVariable()
            stream << variable
            latch.countDown()
            receive {
                variable << 20
            }
        }

        latch.await()
        assert 2 == stream.length()
        assert 10 == stream.poll()?.val
        assert 1 == stream.length()
        assert stream.poll() == null
        thread << 'Proceed'
        assert 20 == stream.val
        assert 0 == stream.length()
        assert stream.poll() == null
    }

    public void testNullValues() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataflowQueue stream = new DataflowQueue()
        final Actor thread = Actors.blockingActor {
            stream << null
            final DataflowVariable variable = new DataflowVariable()
            stream << variable
            latch.countDown()
            receive {
                variable << null
            }
        }

        latch.await()
        assert 2 == stream.length()
        assertNull stream.val
        assert 1 == stream.length()
        thread << 'Proceed'
        assertNull stream.val
        assert 0 == stream.length()
    }

    public void testTake() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataflowQueue stream = new DataflowQueue()
        final Actor thread = Actors.blockingActor {
            final DataflowVariable variable = new DataflowVariable()
            stream << variable
            latch.countDown()
            receive {
                variable << 20
            }
        }

        latch.await()
        assert 1 == stream.length()
        thread << 'Proceed'
        def value = stream.val
        assert 0 == stream.length()
        assert 20 == value
    }

    public void testIteration() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DataflowQueue stream = new DataflowQueue()
        final Actor thread = Actors.blockingActor {
            (0..10).each {stream << it}
            barrier.await()
            receive {
                stream << 11
                barrier.await()
            }
        }

        barrier.await()
        assert 11 == stream.length()
        stream.eachWithIndex {element, index -> assert index == element }
        assert 11 == stream.length()

        thread << 'Proceed'
        barrier.await()
        assert 12 == stream.length()
        (0..10).each {
            assert it == stream.val
        }
    }

    public void testIterationWithNulls() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DataflowQueue stream = new DataflowQueue()
        Actors.blockingActor {
            (0..10).each {stream << null}
            barrier.await()
        }

        barrier.await()
        assert 11 == stream.length()
        stream.each {assertNull it }
        assert 11 == stream.length()

        for (i in (0..10)) { assertNull stream.val }
    }

    public void testToString() {
        final DataflowQueue<Integer> stream = new DataflowQueue<Integer>()
        assert 'DataflowQueue(queue=[])' == stream.toString()
        stream << 10
        assert 'DataflowQueue(queue=[DataflowVariable(value=10)])' == stream.toString()
        stream << 20
        assert 'DataflowQueue(queue=[DataflowVariable(value=10), DataflowVariable(value=20)])' == stream.toString()
        stream.val
        assert 'DataflowQueue(queue=[DataflowVariable(value=20)])' == stream.toString()
        stream.val
        assert 'DataflowQueue(queue=[])' == stream.toString()
        final DataflowVariable variable = new DataflowVariable()
        stream << variable
        assert 'DataflowQueue(queue=[DataflowVariable(value=null)])' == stream.toString()
        variable << '30'
        Thread.sleep 1000  //let the value propagate asynchronously into the variable stored in the stream
        assert 'DataflowQueue(queue=[DataflowVariable(value=30)])' == stream.toString()
        assert 'DataflowQueue(queue=[DataflowVariable(value=30)])' == stream.toString()
        stream.val
        assert 'DataflowQueue(queue=[])' == stream.toString()
        assert 'DataflowQueue(queue=[])' == stream.toString()
    }

    public void testWhenBound() {
        final DataflowQueue stream = new DataflowQueue()
        final Dataflows df = new Dataflows()
        stream >> {df.x1 = it}
        stream >> {df.x2 = it}
        def actor = Actors.actor {
            react {
                df.x3 = it
            }
        }
        stream.whenBound(actor)
        stream << 10
        stream << 20
        stream << 30
        assert 10 == df.x1
        assert 20 == df.x2
        assert 30 == df.x3
    }

    public void testWheneverBound() {
        final DataflowQueue stream = new DataflowQueue()
        final DataflowQueue dfs1 = new DataflowQueue()
        final DataflowQueue dfs2 = new DataflowQueue()
        final DataflowQueue dfs3 = new DataflowQueue()
        stream.wheneverBound {dfs1 << it}
        stream.wheneverBound {dfs2 << it}
        def actor = Actors.actor {
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
        def df = new DataflowVariable()
        stream << df
        df << 40
        assert [10, 20, 30, 40] as Set == [dfs1.val, dfs1.val, dfs1.val, dfs1.val] as Set
        assert [10, 20, 30, 40] as Set == [dfs2.val, dfs2.val, dfs2.val, dfs2.val] as Set
        assert [10, 20, 30, 40] as Set == [dfs3.val, dfs3.val, dfs3.val, dfs3.val] as Set
    }

    public void testAsyncValueRetrieval() {
        def result = new Dataflows()
        final DataflowQueue stream = new DataflowQueue()
        Actors.actor {
            stream << 10
        }
        def handler = Actors.actor {
            react {result.value = it}
        }
        stream.getValAsync(handler)
        assert result.value == 10
    }

    public void testGetValWithTimeout() {
        final DataflowQueue stream = new DataflowQueue()
        final CyclicBarrier barrier = new CyclicBarrier(2)
        Actors.actor {
            stream << 10
            barrier.await()
        }
        barrier.await()
        assert stream.getVal(10, TimeUnit.DAYS) == 10
        assert stream.getVal(3, TimeUnit.SECONDS) == null
        assert stream.getVal(3, TimeUnit.SECONDS) == null
    }

    public void testMissedTimeout() {
        final DataflowQueue stream = new DataflowQueue()
        assertNull stream.getVal(10, TimeUnit.MILLISECONDS)
        stream << 10
        assert 10 == stream.getVal(10, TimeUnit.MILLISECONDS)
        stream << 20
        stream << 30
        assert 20 == stream.getVal(10, TimeUnit.MILLISECONDS)
        assert 30 == stream.getVal(10, TimeUnit.MILLISECONDS)
        assertNull stream.getVal(10, TimeUnit.MILLISECONDS)
        stream << 40
        assert 40 == stream.getVal(10, TimeUnit.MILLISECONDS)
    }

    public void testMissedTimeoutWithNull() {
        final DataflowQueue stream = new DataflowQueue()
        assertNull stream.getVal(10, TimeUnit.MILLISECONDS)
        stream << null
        assert null == stream.getVal(10, TimeUnit.MINUTES)
        stream << null
        stream << 30
        assert null == stream.getVal(10, TimeUnit.MILLISECONDS)
        assert 30 == stream.getVal(10, TimeUnit.MILLISECONDS)
        assertNull stream.getVal(10, TimeUnit.MILLISECONDS)
        stream << null
        assert null == stream.getVal(10, TimeUnit.MILLISECONDS)
    }

    public void testChainWithParams() {

        final DataflowQueue queue = new DataflowQueue()
        def params = new HashMap();
        def result = queue.chainWith(params) { it }
        queue << 1
        assert result.val == 1

    }
}
