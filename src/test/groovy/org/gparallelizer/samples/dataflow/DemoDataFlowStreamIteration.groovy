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

import java.util.concurrent.CyclicBarrier
import org.gparallelizer.dataflow.DataFlow
import org.gparallelizer.dataflow.DataFlowStream

/**
 * This demo shows the ways to work with DataFlowStream.
 * It demonstrates the iterative methods, which use the current snapshot of the stream,
 * as well as the 'val' property to gradually take elements away from the stream.
 */
final CyclicBarrier barrier = new CyclicBarrier(2)

final DataFlowStream stream = new DataFlowStream()
DataFlow.start {
    (0..10).each {stream << it}
    barrier.await()
    react {
        stream << 11
    }
}

barrier.await()
println 'Current snapshot:'
stream.each {print "$it " }
println ''

stream << 11
stream << 12

println 'Another snapshot:'
stream.each {print "$it " }
println ''

println 'Reading from the stream'
(1..stream.length()).each {print "${stream.val} "}
println ''
println "The stream is now empty. Length =  ${stream.length()}"

System.exit 0
