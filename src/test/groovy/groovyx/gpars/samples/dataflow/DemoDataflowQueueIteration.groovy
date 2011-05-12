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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowQueue
import java.util.concurrent.CyclicBarrier

/**
 * This demo shows the ways to work with DataflowQueue.
 * It demonstrates the iterative methods, which use the current snapshot of the stream,
 * as well as the 'val' property to gradually take elements away from the stream.
 */
final CyclicBarrier barrier = new CyclicBarrier(2)

final DataflowQueue stream = new DataflowQueue()
Dataflow.task {
    (0..10).each {stream << it}
    barrier.await()
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

