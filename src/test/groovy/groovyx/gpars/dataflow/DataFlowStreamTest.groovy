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

package groovyx.gpars.dataflow

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

public class DataFlowQueueTest extends GroovyTestCase {

    public void testStream() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataFlowQueue stream = new DataFlowQueue()
        final Actor thread = DataFlow.start {
            stream << 10
            final DataFlowVariable variable = new DataFlowVariable()
            stream << variable
            latch.countDown()
            react {
                variable << 20
            }
        }

        latch.await()
        assertEquals 2, stream.length()
        assertEquals 10, stream.val
        assertEquals 1, stream.length()
        thread << 'Proceed'
        assertEquals 20, stream.val
        assertEquals 0, stream.length()
    }

    public void testStreamPoll() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataFlowQueue stream = new DataFlowQueue()
        assert stream.poll() == null
        assert stream.poll() == null
        stream << 1
        assert stream.poll()?.val == 1
        assert stream.poll()?.val == null
        stream << 2
        assert stream.poll()?.val == 2
        assert stream.poll()?.val == null
        final Actor thread = DataFlow.start {
            stream << 10
            final DataFlowVariable variable = new DataFlowVariable()
            stream << variable
            latch.countDown()
            react {
                variable << 20
            }
        }

        latch.await()
        assertEquals 2, stream.length()
        assertEquals 10, stream.poll()?.val
        assertEquals 1, stream.length()
        assert stream.poll() == null
        thread << 'Proceed'
        assertEquals 20, stream.val
        assertEquals 0, stream.length()
        assert stream.poll() == null
    }

    public void testNullValues() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataFlowQueue stream = new DataFlowQueue()
        final Actor thread = DataFlow.start {
            stream << null
            final DataFlowVariable variable = new DataFlowVariable()
            stream << variable
            latch.countDown()
            react {
                variable << null
            }
        }

        latch.await()
        assertEquals 2, stream.length()
        assertEquals null, stream.val
        assertEquals 1, stream.length()
        thread << 'Proceed'
        assertEquals null, stream.val
        assertEquals 0, stream.length()
    }

    public void testTake() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataFlowQueue stream = new DataFlowQueue()
        final Actor thread = DataFlow.start {
            final DataFlowVariable variable = new DataFlowVariable()
            stream << variable
            latch.countDown()
            react {
                variable << 20
            }
        }

        latch.await()
        assertEquals 1, stream.length()
        thread << 'Proceed'
        def value = stream.val
        assertEquals 0, stream.length()
        assertEquals 20, value
    }

    public void testIteration() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DataFlowQueue stream = new DataFlowQueue()
        final Actor thread = DataFlow.start {
            (0..10).each {stream << it}
            barrier.await()
            react {
                stream << 11
                barrier.await()
            }
        }

        barrier.await()
        assertEquals 11, stream.length()
        stream.eachWithIndex {index, element -> assertEquals index, element }
        assertEquals 11, stream.length()

        thread << 'Proceed'
        barrier.await()
        assertEquals 12, stream.length()
        (0..10).each {
            assertEquals it, stream.val
        }
    }

    public void testIterationWithNulls() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DataFlowQueue stream = new DataFlowQueue()
        DataFlow.start {
            (0..10).each {stream << null}
            barrier.await()
        }

        barrier.await()
        assertEquals 11, stream.length()
        stream.each {assertNull it }
        assertEquals 11, stream.length()

        for (i in (0..10)) { assertNull stream.val }
    }

    public void testToString() {
        final DataFlowQueue<Integer> stream = new DataFlowQueue<Integer>()
        assertEquals 'DataFlowQueue(queue=[])', stream.toString()
        stream << 10
        assertEquals 'DataFlowQueue(queue=[DataFlowVariable(value=10)])', stream.toString()
        stream << 20
        assertEquals 'DataFlowQueue(queue=[DataFlowVariable(value=10), DataFlowVariable(value=20)])', stream.toString()
        stream.val
        assertEquals 'DataFlowQueue(queue=[DataFlowVariable(value=20)])', stream.toString()
        stream.val
        assertEquals 'DataFlowQueue(queue=[])', stream.toString()
        final DataFlowVariable variable = new DataFlowVariable()
        stream << variable
        assertEquals 'DataFlowQueue(queue=[DataFlowVariable(value=null)])', stream.toString()
        variable << '30'
        Thread.sleep 1000  //let the value propagate asynchronously into the variable stored in the stream
        assertEquals 'DataFlowQueue(queue=[DataFlowVariable(value=30)])', stream.toString()
        assertEquals 'DataFlowQueue(queue=[DataFlowVariable(value=30)])', stream.toString()
        stream.val
        assertEquals 'DataFlowQueue(queue=[])', stream.toString()
        assertEquals 'DataFlowQueue(queue=[])', stream.toString()
    }

    public void testWhenBound() {
        final DataFlowQueue stream = new DataFlowQueue()
        final DataFlows df = new DataFlows()
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
        assertEquals 10, df.x1
        assertEquals 20, df.x2
        assertEquals 30, df.x3
    }

    public void testWheneverBound() {
        final DataFlowQueue stream = new DataFlowQueue()
        final DataFlowQueue dfs1 = new DataFlowQueue()
        final DataFlowQueue dfs2 = new DataFlowQueue()
        final DataFlowQueue dfs3 = new DataFlowQueue()
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
        def df = new DataFlowVariable()
        stream << df
        df << 40
        assert [10, 20, 30, 40] as Set == [dfs1.val, dfs1.val, dfs1.val, dfs1.val] as Set
        assert [10, 20, 30, 40] as Set == [dfs2.val, dfs2.val, dfs2.val, dfs2.val] as Set
        assert [10, 20, 30, 40] as Set == [dfs3.val, dfs3.val, dfs3.val, dfs3.val] as Set
    }

    public void testAsyncValueRetrieval() {
        def result = new DataFlows()
        final DataFlowQueue stream = new DataFlowQueue()
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
        final DataFlowQueue stream = new DataFlowQueue()
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
        final DataFlowQueue stream = new DataFlowQueue()
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
        final DataFlowQueue stream = new DataFlowQueue()
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
}
