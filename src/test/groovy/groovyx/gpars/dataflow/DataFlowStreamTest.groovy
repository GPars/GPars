// GPars (formerly GParallelizer)
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

public class DataFlowStreamTest extends GroovyTestCase {

    public void testStream() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataFlowStream stream = new DataFlowStream()
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

    public void testNullValues() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataFlowStream stream = new DataFlowStream()
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

        final DataFlowStream stream = new DataFlowStream()
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

        final DataFlowStream stream = new DataFlowStream()
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

        final DataFlowStream stream = new DataFlowStream()
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
        final DataFlowStream<Integer> stream = new DataFlowStream<Integer>()
        assertEquals 'DataFlowStream(queue=[])', stream.toString()
        stream << 10
        assertEquals 'DataFlowStream(queue=[DataFlowVariable(value=10)])', stream.toString()
        stream << 20
        assertEquals 'DataFlowStream(queue=[DataFlowVariable(value=10), DataFlowVariable(value=20)])', stream.toString()
        stream.val
        assertEquals 'DataFlowStream(queue=[DataFlowVariable(value=20)])', stream.toString()
        stream.val
        assertEquals 'DataFlowStream(queue=[])', stream.toString()
        final DataFlowVariable variable = new DataFlowVariable()
        stream << variable
        assertEquals 'DataFlowStream(queue=[DataFlowVariable(value=null)])', stream.toString()
        variable << '30'
        Thread.sleep 1000  //let the value propagate asynchronously into the variable stored in the stream
        assertEquals 'DataFlowStream(queue=[DataFlowVariable(value=30)])', stream.toString()
        assertEquals 'DataFlowStream(queue=[DataFlowVariable(value=30)])', stream.toString()
        stream.val
        assertEquals 'DataFlowStream(queue=[])', stream.toString()
        assertEquals 'DataFlowStream(queue=[])', stream.toString()
    }

    public void testWhenNextBound() {
        final DataFlowStream stream = new DataFlowStream()
        final DataFlows df = new DataFlows()
        stream >> {df.x1 = it}
        stream >> {df.x2 = it}
        def actor = Actors.actor {
            react {
                df.x3 = it
            }
        }
        stream.whenNextBound(actor)
        stream << 10
        stream << 20
        stream << 30
        assertEquals 10, df.x1
        assertEquals 20, df.x2
        assertEquals 30, df.x3
    }

    public void testWhenBound() {
        final DataFlowStream stream = new DataFlowStream()
        final DataFlowStream dfs1 = new DataFlowStream()
        final DataFlowStream dfs2 = new DataFlowStream()
        final DataFlowStream dfs3 = new DataFlowStream()
        stream.whenBound {dfs1 << it}
        stream.whenBound {dfs2 << it}
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
        stream.whenBound(actor)
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
}
