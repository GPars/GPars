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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.DataFlowQueue
import static groovyx.gpars.dataflow.DataFlow.task

/**
 * A producer consumer sample, where the producer generates numbers into the DataFlowQueue, the intermediate consumer
 * keeps reading numbers from the producer, calculates the summary of numbers it saw so far and passes the summaries
 * on to the final consumer, which prints them out.
 * Since both consumers read elements using the 'val' property, they will keep reading until stopped explicitly.
 */
void ints(int n, int max, DataFlowQueue<Integer> stream) {
    if (n != max) {
        println "Generating int: $n"
        stream << n
        ints(n + 1, max, stream)
    }
}

@SuppressWarnings("GroovyInfiniteRecursion")
void sum(int s, DataFlowQueue<Integer> inStream, DataFlowQueue<Integer> outStream) {
    println "Calculating $s"
    outStream << s
    sum(inStream.val + s, inStream, outStream)
}

void printSum(DataFlowQueue stream) {
    println "Result ${stream.val}"
    printSum stream
}

final def producer = new DataFlowQueue<Integer>()
final def consumer = new DataFlowQueue<Integer>()

task {
    ints(0, 1000, producer)
}

task {
    sum(0, producer, consumer)
}

task {
    printSum(consumer)
}

System.in.read()
System.exit 0
