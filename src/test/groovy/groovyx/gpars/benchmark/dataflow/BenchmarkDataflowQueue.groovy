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

package groovyx.gpars.benchmark.dataflow

import groovyx.gpars.dataflow.DataflowQueue

import java.util.concurrent.CyclicBarrier

final DataflowQueue queue = new DataflowQueue()
final barrier = new CyclicBarrier(2)

final iterationCount = 10000000


4.times {
    final def t1 = System.currentTimeMillis()

    Thread.start {
        barrier.await()
        for (i in (1..iterationCount)) {
            queue << i
        }
    }

    final consumer = Thread.start {
        barrier.await()
        for (i in (1..iterationCount)) {
            queue.val
        }
    }

    consumer.join()
    println(System.currentTimeMillis() - t1)
}

