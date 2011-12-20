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

package groovyx.gpars.dataflow.stream

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Dataflows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

public class DataflowStreamAdapterTest extends GroovyTestCase {

    public void testStream() {
        final CountDownLatch latch = new CountDownLatch(1)
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)

        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        final Actor thread = Actors.actor {
            writeStream << 10
            final DataflowVariable variable = new DataflowVariable()
            writeStream << variable
            latch.countDown()
            react {
                variable << 20
            }
        }

        latch.await()
        assert 10 == stream.val
        thread << 'Proceed'
        assert 20 == stream.val
    }

    public void testStreamPoll() {
        final CountDownLatch latch = new CountDownLatch(1)
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)

        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        assert stream.poll() == null
        assert stream.poll() == null
        writeStream << 1
        assert stream.poll()?.val == 1
        assert stream.poll()?.val == null
        writeStream << 2
        assert stream.poll()?.val == 2
        assert stream.poll()?.val == null
        final Actor thread = Actors.actor {
            writeStream << 10
            final DataflowVariable variable = new DataflowVariable()
            writeStream << variable
            latch.countDown()
            react {
                variable << 20
            }
        }

        latch.await()
        assert 10 == stream.poll()?.val
        assert stream.poll() == null
        thread << 'Proceed'
        assert 20 == stream.val
        assert stream.poll() == null
    }

    public void testNullValues() {
        final CountDownLatch latch = new CountDownLatch(1)
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)

        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        final Actor thread = Actors.blockingActor {
            writeStream << null
            final DataflowVariable variable = new DataflowVariable()
            writeStream << variable
            latch.countDown()
            receive {
                variable << null
            }
        }

        latch.await()
        assertNull stream.val
        thread << 'Proceed'
        assertNull stream.val
    }

    public void testTake() {
        final CountDownLatch latch = new CountDownLatch(1)
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)

        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        final Actor thread = Actors.blockingActor {
            final DataflowVariable variable = new DataflowVariable()
            writeStream << variable
            latch.countDown()
            receive {
                variable << 20
            }
        }

        latch.await()
        thread << 'Proceed'
        def value = stream.val
        assert 20 == value
    }

    public void testIteration() {
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)

        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        Dataflow.task {
            (0..10).each {writeStream << it}
            writeStream << DataflowStream.eos()
        }.join()
        stream.eachWithIndex {index, element -> assert index == element }

        (0..10).each {
            assert it == stream.val
        }
    }

    public void testIterationWithNulls() {
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)

        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        Thread.start() {
            (0..10).each {writeStream << null}
            barrier.await()
        }

        barrier.await()
        stream.each {assertNull it }

        for (i in (0..10)) { assertNull stream.val }
    }

    public void testWhenBound() {
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)
        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        final Dataflows df = new Dataflows()
        stream >> {df.x1 = it}
        stream >> {df.x2 = it}
        def actor = Actors.actor {
            react {
                df.x3 = it
            }
        }
        stream.whenBound(actor)
        writeStream << 10
        writeStream << 20
        writeStream << 30
        assert 10 == df.x1
        assert 20 == df.x2
        assert 30 == df.x3
    }

    public void testWheneverBound() {
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)

        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
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
        writeStream << 10
        writeStream << 20
        writeStream << 30
        def df = new DataflowVariable()
        writeStream << df
        df << 40
        assert [10, 20, 30, 40] as Set == [dfs1.val, dfs1.val, dfs1.val, dfs1.val] as Set
        assert [10, 20, 30, 40] as Set == [dfs2.val, dfs2.val, dfs2.val, dfs2.val] as Set
        assert [10, 20, 30, 40] as Set == [dfs3.val, dfs3.val, dfs3.val, dfs3.val] as Set
    }

    public void testAsyncValueRetrieval() {
        def result = new Dataflows()
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)
        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        Actors.actor {
            writeStream << 10
        }
        def handler = Actors.actor {
            react {result.value = it}
        }
        stream.getValAsync(handler)
        assert result.value == 10
    }

    public void testGetValWithTimeout() {
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)
        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        final CyclicBarrier barrier = new CyclicBarrier(2)
        Actors.actor {
            writeStream << 10
            barrier.await()
        }
        barrier.await()
        assert stream.getVal(10, TimeUnit.DAYS) == 10
        assert stream.getVal(3, TimeUnit.SECONDS) == null
        assert stream.getVal(3, TimeUnit.SECONDS) == null
    }

    public void testMissedTimeout() {
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)
        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        assertNull stream.getVal(10, TimeUnit.MILLISECONDS)
        writeStream << 10
        assert 10 == stream.getVal(10, TimeUnit.MILLISECONDS)
        writeStream << 20
        writeStream << 30
        assert 20 == stream.getVal(10, TimeUnit.MILLISECONDS)
        assert 30 == stream.getVal(10, TimeUnit.MILLISECONDS)
        assertNull stream.getVal(10, TimeUnit.MILLISECONDS)
        writeStream << 40
        assert 40 == stream.getVal(10, TimeUnit.MILLISECONDS)
    }

    public void testMissedTimeoutWithNull() {
        final def original = new DataflowStream()
        final def writeStream = new DataflowStreamWriteAdapter(original)
        final DataflowReadChannel stream = new DataflowStreamReadAdapter(original)
        assertNull stream.getVal(10, TimeUnit.MILLISECONDS)
        writeStream << null
        assert null == stream.getVal(10, TimeUnit.MINUTES)
        writeStream << null
        writeStream << 30
        assert null == stream.getVal(10, TimeUnit.MILLISECONDS)
        assert 30 == stream.getVal(10, TimeUnit.MILLISECONDS)
        assertNull stream.getVal(10, TimeUnit.MILLISECONDS)
        writeStream << null
        assert null == stream.getVal(10, TimeUnit.MILLISECONDS)
    }
}
