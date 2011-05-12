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

import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import java.util.concurrent.CyclicBarrier

public class DataflowStreamBroadCastTest extends GroovyTestCase {

    public void testMultipleThreadedWrite() {
        final DataflowWriteChannel writeStream = new DataflowBroadcast()
        final DataflowReadChannel stream = writeStream.createReadChannel()

        final CyclicBarrier barrier = new CyclicBarrier(10)
        10.times {value ->
            Dataflow.task {
                barrier.await()
                writeStream << value
            }
        }
        checkResult(stream)
    }

    public void testMultipleReaders() {
        final DataflowWriteChannel writeStream = new DataflowBroadcast()
        final DataflowReadChannel stream1 = writeStream.createReadChannel()
        final DataflowReadChannel stream2 = writeStream.createReadChannel()

        final CyclicBarrier barrier = new CyclicBarrier(10)
        10.times {value ->
            Dataflow.task {
                barrier.await()
                writeStream << value
            }
        }
        checkResult(stream1)
        checkResult(stream2)
    }

    public void testTwoStreams() {
        final DataflowWriteChannel broadcastStream = new DataflowBroadcast()
        final DataflowReadChannel stream1 = broadcastStream.createReadChannel()
        final DataflowReadChannel stream2 = broadcastStream.createReadChannel()
        broadcastStream << 'Message1'
        broadcastStream << 'Message2'
        broadcastStream << 'Message3'
        assert stream1.val == stream2.val
        assert stream1.val == stream2.val
        assert stream1.val == 'Message3'
        assert stream2.val == 'Message3'
    }

    private def checkResult(DataflowReadChannel stream) {
        def result = (1..10).collect {
            stream.val
        }.sort()
        assert result == (0..9).collect {it}
    }
}
