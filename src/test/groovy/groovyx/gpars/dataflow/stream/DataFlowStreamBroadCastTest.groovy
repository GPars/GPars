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

package groovyx.gpars.dataflow.stream

import groovyx.gpars.dataflow.DataFlow
import groovyx.gpars.dataflow.DataFlowBroadcast
import groovyx.gpars.dataflow.DataFlowReadChannel
import groovyx.gpars.dataflow.DataFlowWriteChannel
import java.util.concurrent.CyclicBarrier

public class DataFlowStreamBroadCastTest extends GroovyTestCase {

    public void testMultipleThreadedWrite() {
        final DataFlowWriteChannel writeStream = new DataFlowBroadcast()
        final DataFlowReadChannel stream = writeStream.createReadChannel()

        final CyclicBarrier barrier = new CyclicBarrier(10)
        10.times {value ->
            DataFlow.task {
                barrier.await()
                writeStream << value
            }
        }
        checkResult(stream)
    }

    public void testMultipleReaders() {
        final DataFlowWriteChannel writeStream = new DataFlowBroadcast()
        final DataFlowReadChannel stream1 = writeStream.createReadChannel()
        final DataFlowReadChannel stream2 = writeStream.createReadChannel()

        final CyclicBarrier barrier = new CyclicBarrier(10)
        10.times {value ->
            DataFlow.task {
                barrier.await()
                writeStream << value
            }
        }
        checkResult(stream1)
        checkResult(stream2)
    }

    public void testTwoStreams() {
        final DataFlowWriteChannel broadcastStream = new DataFlowBroadcast()
        final DataFlowReadChannel stream1 = broadcastStream.createReadChannel()
        final DataFlowReadChannel stream2 = broadcastStream.createReadChannel()
        broadcastStream << 'Message1'
        broadcastStream << 'Message2'
        broadcastStream << 'Message3'
        assert stream1.val == stream2.val
        assert stream1.val == stream2.val
        assert stream1.val == 'Message3'
        assert stream2.val == 'Message3'
    }

    private def checkResult(DataFlowReadChannel stream) {
        def result = (1..10).collect {
            stream.val
        }.sort()
        assert result == (0..9).collect {it}
    }
}
