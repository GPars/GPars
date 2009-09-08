//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowStream
import static org.gparallelizer.dataflow.DataFlow.start

/**
 * A producer consumer sample, where the producer generates numbers into the DataFlowStream and the consumer
 * takes a snapshot of the DataFlowStream using the collect() method to calculate summary of the numbers in the stream.
 */

void ints(int n, int max, DataFlowStream<Integer> stream) {
    if (n != max) {
        println "Generating int: $n"
        stream << n
        ints(n+1, max, stream)
    }
}

final def producer = new DataFlowStream<Integer>()

start {
    ints(0, 1000, producer)
}

start {
    Thread.sleep(1000)
    println "Sum: ${producer.collect{it * it}.inject(0){sum, x -> sum + x}}"
    System.exit 0
}
