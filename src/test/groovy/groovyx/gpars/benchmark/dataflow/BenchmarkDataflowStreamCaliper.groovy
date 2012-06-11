// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.benchmark.dataflow

import com.google.caliper.Runner
import groovyx.gpars.group.PGroup
import groovyx.gpars.dataflow.DataflowChannel
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import java.util.concurrent.CountDownLatch
import com.google.caliper.Param
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.stream.DataflowStream
import groovyx.gpars.dataflow.stream.DataflowStreamWriteAdapter
import groovyx.gpars.dataflow.stream.DataflowStreamReadAdapter
import com.google.caliper.SimpleBenchmark

/**
 * Created with IntelliJ IDEA.
 * User: don
 * Date: 6/6/12
 * Time: 3:37 PM
 * To change this template use File | Settings | File Templates.
 */
class BenchmarkDataflowStreamCaliper extends SimpleBenchmark{
    @Param (["1","2","3","4"]) int numConsumer
    @Param (["1","2","3","4"]) int numProducer

    final def group1 = new DefaultPGroup(4)
    final def group2 = new DefaultPGroup(4)
    final def stream = new DataflowQueue()

    final DataflowStream dfStream = new DataflowStream()
    final DataflowStreamWriteAdapter adapterForWrite = new DataflowStreamWriteAdapter(dfStream)
    final DataflowStreamReadAdapter adapterForRead = new DataflowStreamReadAdapter(dfStream)

    def perform(PGroup producerGroup, PGroup consumerGroup, numberOfProducers, numberOfConsumers, numberOfMessages, DataflowChannel stream) {
        perform(producerGroup, consumerGroup, numberOfProducers, numberOfConsumers, numberOfMessages, stream, stream)

    }

    def perform(PGroup producerGroup, PGroup consumerGroup, numberOfProducers, numberOfConsumers, numberOfMessages, DataflowReadChannel streamToRead, DataflowWriteChannel streamToWrite) {
        assert !streamToRead.bound
        final long numberOfMessagesPerProducer = numberOfMessages / numberOfProducers

        final def finishedSignal = new CountDownLatch(numberOfProducers)
        final def startSignal = new CountDownLatch(1)
        numberOfConsumers.times {
            consumerGroup.task {
                def value = streamToRead.val
                while (value != null) {
                    if (value == -1){
                        finishedSignal.countDown()
                    }
                    value = streamToRead.val
                }
            }
        }

        numberOfProducers.times {num ->
            producerGroup.task {
                startSignal.await()
                for (int i = 0; i < numberOfMessagesPerProducer; i++) {
                    streamToWrite.bind num
                }
                streamToWrite.bind(-1)
            }
        }

        def l1 = System.currentTimeMillis()
        startSignal.countDown()
        finishedSignal.await()
        def l2 = System.currentTimeMillis()

        numberOfConsumers.times {
            streamToWrite.bind null
        }


    }
    public void timeDataflowStream(int reps){
        reps.times{
            perform(group1, group2, numProducer, numConsumer, 1000000, stream)
        }
    }
}
