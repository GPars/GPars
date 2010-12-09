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
import groovyx.gpars.dataflow.DataFlowReadChannel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

public class DataFlowStreamWriteAdapterTest extends GroovyTestCase {

    public void testMultipleThreadedWrite() {
        final CountDownLatch latch = new CountDownLatch(1)
        final def original = new DataFlowStream()
        final def writeStream = new DataFlowStreamWriteAdapter(original)
        final DataFlowReadChannel stream = new DataFlowStreamReadAdapter(original)

        final CyclicBarrier barrier = new CyclicBarrier(10)
        10.times {value ->
            DataFlow.task {
                barrier.await()
                writeStream << value
            }
        }
        def result = (1..10).collect {
            stream.val
        }.sort()
        assert result == (0..9).collect {it}
    }
}
