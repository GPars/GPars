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

package groovyx.gpars.benchmark.dataflow

import groovyx.gpars.dataflow.DataFlowChannel
import groovyx.gpars.dataflow.DataFlowQueue
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup
import java.util.concurrent.CountDownLatch

final def group1 = new DefaultPGroup(4)
final def group2 = new DefaultPGroup(4)
final def stream = new DataFlowQueue()

//perform(group1, group2, 4, 4, 10, new Stream())

perform(group1, group2, 4, 4, 4000000, stream)
perform(group1, group2, 4, 4, 4000000, stream)
perform(group1, group2, 4, 2, 4000000, stream)
perform(group1, group2, 6, 2, 4000000, stream)
perform(group1, group2, 2, 4, 4000000, stream)
perform(group1, group2, 2, 6, 4000000, stream)
perform(group1, group2, 1, 6, 4000000, stream)

def perform(PGroup producerGroup, PGroup consumerGroup, numberOfProducers, numberOfConsumers, numberOfMessages, DataFlowChannel stream) {
    assert !stream.bound
    final long numberOfMessagesPerProducer = numberOfMessages / numberOfProducers

    final def finishedSignal = new CountDownLatch(numberOfProducers)
    final def startSignal = new CountDownLatch(1)
    numberOfConsumers.times {
        consumerGroup.task {
            def value = stream.val
            while (value != null) {
                if (value == -1) finishedSignal.countDown()
                value = stream.val
            }
        }
    }

    numberOfProducers.times {num ->
        producerGroup.task {
            startSignal.await()
            for (int i = 0; i < numberOfMessagesPerProducer; i++) {
                stream.bind num
            }
            stream.bind(-1)
        }
    }

    def l1 = System.currentTimeMillis()
    startSignal.countDown()
    finishedSignal.await()
    def l2 = System.currentTimeMillis()

    numberOfConsumers.times {
        stream.bind null
    }

    println "Number of messages: ${numberOfMessages}"
    println "Number of producers: ${numberOfProducers}"
    println "Number of consumers: ${numberOfConsumers}"
    println "Time: ${l2 - l1}"
}